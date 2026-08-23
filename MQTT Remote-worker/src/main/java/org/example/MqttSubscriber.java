package org.example;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;

public class MqttSubscriber implements Runnable, MqttCallback {

    private final MqttClient client;
    private final String workerId;
    private final RemoteWorker worker;
    private final Semaphore readySignal;

    public MqttSubscriber(MqttClient client, String workerId, RemoteWorker worker, Semaphore readySignal) {
        this.client = client;
        this.workerId = workerId;
        this.worker = worker;
        this.readySignal = readySignal;
    }

    @Override
    public void run() {
        try {
            client.setCallback(this);
            String jobTopic = Protocol.JOB_TOPIC_PREFIX + workerId;

            client.subscribe(jobTopic, 1);
            System.out.println("Subscribed to " + jobTopic);

        } catch (Exception exception) {
            System.err.println("Subscriber failed: " + exception.getMessage());
        } finally {
            readySignal.release();
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String messageText = new String(message.getPayload(), StandardCharsets.UTF_8).trim();
        try {
            Operation operation = Protocol.decodeJob(messageText);
            worker.receive(operation);

        } catch (Exception exception) {
            System.err.println("Ignored malformed job: " + exception.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        String errorMessage = "Unknown connection error";

        if (cause != null && cause.getMessage() != null) {
            errorMessage = cause.getMessage();
        }

        System.err.println("MQTT connection lost: " + errorMessage);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}