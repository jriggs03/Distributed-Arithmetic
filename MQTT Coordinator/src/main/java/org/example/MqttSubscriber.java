package org.example;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;

public class MqttSubscriber implements Runnable, MqttCallback {

    private final MqttClient client;
    private final Outsourcer outsourcer;
    private final ConsoleView view;
    private final Semaphore readySignal;

    public MqttSubscriber(MqttClient client, Outsourcer outsourcer, ConsoleView view, Semaphore readySignal) {
        this.client = client;
        this.outsourcer = outsourcer;
        this.view = view;
        this.readySignal = readySignal;
    }

    @Override
    public void run() {
        try {
            client.setCallback(this);
            client.subscribe(Protocol.AVAILABLE_TOPIC, 1);
            client.subscribe(Protocol.RESULTS_TOPIC, 1);
            readySignal.release();
            view.info("Subscribed to worker availability and result topics");

        } catch (Exception exception) {
            view.info("Subscriber failed: " + exception.getMessage());
            readySignal.release();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        String message = "Unknown connection error";
        if (cause != null && cause.getMessage() != null) {
            message = cause.getMessage();
        }
        view.info("MQTT connection lost: " + message);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {

        String messageText = new String(message.getPayload(), StandardCharsets.UTF_8).trim();
        try {
            if (Protocol.AVAILABLE_TOPIC.equals(topic)) {
                handleWorkerAvailable(messageText);
            } else if (Protocol.RESULTS_TOPIC.equals(topic)) {
                handleResultReceived(messageText);
            }

        } catch (RuntimeException exception) {
            view.info("Ignored malformed MQTT message: " + exception.getMessage()
            );
        }
    }

    private void handleWorkerAvailable(String workerId) {
        if (workerId.isBlank()) {
            view.info("Ignored availability message with no worker ID");
            return;
        }
        outsourcer.workerAvailable(workerId);
    }

    private void handleResultReceived(String messageText) {
        Protocol.ResultMessage result = Protocol.decodeResult(messageText);
        outsourcer.resultReceived(result);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}