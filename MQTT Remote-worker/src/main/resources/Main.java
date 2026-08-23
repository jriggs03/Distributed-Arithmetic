package org.example;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class Main {

    private Main() {
    }
    public static void main(String[] args) throws Exception {
        String workerId;

        if (args.length > 0) {
            workerId = args[0];
        } else {
            workerId = "worker-" + UUID.randomUUID().toString().substring(0, 4);
        }

        String broker;

        if (args.length > 1) {
            broker = args[1];
        } else {
            broker = "tcp://localhost:1883";
        }

        long solveDelayMillis;

        if (args.length > 2) {
            solveDelayMillis = Long.parseLong(args[2]);
        } else {
            solveDelayMillis = 1000;
        }

        String clientId = workerId + "-" + UUID.randomUUID();

        MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());

        MqttConnectOptions connectOptions = new MqttConnectOptions();

        connectOptions.setCleanSession(true);
        connectOptions.setAutomaticReconnect(true);

        client.connect(connectOptions);

        MqttPublisher publisher = new MqttPublisher(client);

        RemoteWorker worker =
                new RemoteWorker(
                        workerId,
                        publisher,
                        solveDelayMillis
                );

        Semaphore subscriptionReady = new Semaphore(0);

        MqttSubscriber mqttSubscriber = new MqttSubscriber(
                        client,
                        workerId,
                        worker,
                        subscriptionReady
                );

        Thread subscriberThread = new Thread(
                        mqttSubscriber,
                        "mqtt-subscriber");

        subscriberThread.start();
        subscriptionReady.acquire();

        Thread workerThread =
                new Thread(
                        worker,
                        workerId
                );

        workerThread.start();
        System.out.println("RemoteWorker " + workerId + " connected to " + broker);
        System.out.println("Press Ctrl+C to stop");

        Runtime.getRuntime().addShutdownHook(
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {

                                workerThread.interrupt();
                                subscriberThread.interrupt();

                                try {
                                    if (client.isConnected()) {
                                        client.disconnect();
                                    }

                                    client.close();

                                } catch (Exception exception) {
                                    System.err.println(
                                            "Shutdown error: "
                                                    + exception.getMessage()
                                    );
                                }
                            }
                        }, "shutdown-hook"
                )
        );

        CountDownLatch keepRunning = new CountDownLatch(1);
        keepRunning.await();
    }
}