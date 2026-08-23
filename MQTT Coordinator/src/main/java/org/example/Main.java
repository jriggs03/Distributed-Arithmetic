package org.example;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {
        String brokerAddress = "tcp://localhost:1883";
        int localWorkerCount = 2;
        long timeoutMillis = 5000;

        if (args.length > 0) {
            brokerAddress = args[0];
        }
        if (args.length > 1) {
            localWorkerCount = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            timeoutMillis = Long.parseLong(args[2]);
        }

        ConsoleView view = new ConsoleView();
        OperationRepository repository = new OperationRepository(100);
        NoteTaker noteTaker = new NoteTaker();

        String clientId = "coordinator-" + UUID.randomUUID();

        MqttClient client = new MqttClient(brokerAddress, clientId, new MemoryPersistence());

        MqttConnectOptions connectionOptions = new MqttConnectOptions();

        connectionOptions.setCleanSession(true);
        connectionOptions.setAutomaticReconnect(true);

        client.connect(connectionOptions);

        MqttPublisher publisher = new MqttPublisher(client);

        Outsourcer outsourcer = new Outsourcer(repository, publisher, noteTaker, view, timeoutMillis);

        Semaphore subscriptionReady = new Semaphore(0);

        MqttSubscriber subscriber = new MqttSubscriber(client, outsourcer, view, subscriptionReady);

        Thread subscriberThread = new Thread(subscriber, "mqtt-subscriber");

        subscriberThread.start();

        subscriptionReady.acquire();

        List<Thread> applicationThreads = new ArrayList<>();

        Producer producer = new Producer(repository, view, 750);

        Thread producerThread = new Thread(producer, "producer");

        applicationThreads.add(producerThread);

        for (int workerNumber = 1; workerNumber <= localWorkerCount; workerNumber++) {
            String workerName = "LOCAL-" + workerNumber;

            LocalWorker localWorker = new LocalWorker(workerName, repository, view, 1400);

            Thread workerThread = new Thread(localWorker, "local-worker-" + workerNumber);

            applicationThreads.add(workerThread);
        }

        for (Thread thread : applicationThreads) {
            thread.start();
        }

        ScheduledExecutorService statisticsExecutor = Executors.newSingleThreadScheduledExecutor();

        statisticsExecutor.scheduleAtFixedRate(
                () -> view.status(repository.getSize(), noteTaker.getSize()),5,5, TimeUnit.SECONDS);

        view.info("Coordinator running at " + brokerAddress + " with " + localWorkerCount + " local workers");

        view.info("Press Ctrl+C to stop");

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    for (Thread thread : applicationThreads) {
                        thread.interrupt();
                    }
                    subscriberThread.interrupt();
                    statisticsExecutor.shutdownNow();
                    outsourcer.stop();
                    try {
                        if (client.isConnected()) {
                            client.disconnect();
                        }
                        client.close();
                    } catch (Exception exception) {
                        view.info("Error while shutting down MQTT client: " + exception.getMessage());
                    }
                })
        );
        new CountDownLatch(1).await();
    }
}