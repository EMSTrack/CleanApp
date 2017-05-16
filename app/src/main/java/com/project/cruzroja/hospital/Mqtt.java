package com.project.cruzroja.hospital;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Created by Fabian Choi on 5/12/2017.
 */

public class Mqtt implements MqttCallbackExtended {
    private static final String TAG = com.project.cruzroja.hospital.Mqtt.class.getSimpleName();

    private final String serverUri = "ssl://cruzroja.ucsd.edu:8883";
    private final String publishTopic = "exampleAndroidPublishTopic"; // TODO: FILL OUT
    private String clientId = "HospitalAppClient-";

    public Mqtt() {
        clientId += System.currentTimeMillis();
        try {
            MqttClient client = new MqttClient(serverUri, clientId, new MemoryPersistence());
            client.setCallback(this);
            client.connect();

            String topic = "topic";
            client.subscribe(topic);

        } catch (MqttException e) {
            e.printStackTrace();
        }

    }
    @Override
    public void connectionLost(Throwable cause) {
        Log.d(TAG, "Connection to broker lost");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        Log.d(TAG, "Message arrived:");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(TAG, "Message delivered successfully");
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        Log.d(TAG, "Connected to the broker successfully");
    }
}
