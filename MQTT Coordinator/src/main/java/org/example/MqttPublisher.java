package org.example;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

public class MqttPublisher {

    private final MqttClient client;

    public MqttPublisher(MqttClient client) {
        this.client = client;
    }

    public synchronized void publishMessage(String topic, String messageText) throws MqttException {
        byte[] payload = messageText.getBytes(StandardCharsets.UTF_8);
        MqttMessage message = new MqttMessage(payload);

        message.setQos(1);
        message.setRetained(false);

        client.publish(topic, message);
    }
}