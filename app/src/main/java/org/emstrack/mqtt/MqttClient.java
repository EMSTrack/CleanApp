package org.emstrack.mqtt;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by Fabian Choi on 5/12/2017.
 * Singleton that connects to the Mqtt broker.
 */
public class MqttClient {

    private static final String TAG = MqttClient.class.getSimpleName();
    private static MqttClient instance;

    private MqttAndroidClient mqttClient;

    private final String serverUri = "ssl://cruzroja.ucsd.edu:8883";
    private String clientId = "HospitalAppClient-";
    private boolean connected = false;

    /* MQTT Client is a thread-safe singleton
     * see: https://www.javaworld.com/article/2073352/core-java/simply-singleton.html
     */

    private MqttClient(Context context) {
        this.clientId += System.currentTimeMillis();
        this.connected = false;

        // Initialize the client
        this.mqttClient = new MqttAndroidClient(context, serverUri, clientId);
    }

    /**
     * Returns existing instance or creates if none exist
     * @param context Context
     * @return Existing or new instance of the client
     */
    public static synchronized MqttClient getInstance(Context context) {
        if(instance == null) {
            instance = new MqttClient(context);
        }
        return instance;
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * Connect the client to the broker with a username and password.
     * To reset the callback, use the setCallback function instead.
     * @param username Username
     * @param password Password
     * @param callback Actions to complete on connection
     */
    public void connect(String username, String password,
                        final MqttConnectCallback connectCallback,
                        final MqttCallbackExtended callback) {

        // if connected, disconnect first
        if (isConnected()) {
            disconnect();
        }

        // Set callback options
        mqttClient.setCallback(callback);

        // Set client options
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(password.toCharArray());

        try {
            mqttClient.connect(mqttConnectOptions, null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Connection to broker successful as clientId = " + clientId);
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttClient.setBufferOpts(disconnectedBufferOptions);
                    connected = true;
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "Connection to broker failed");
                    Log.e(TAG, exception.getMessage());
                    connected = false;

                    // Set alert if login is wrong
                    //Toast.makeText(context, "Wrong login information!", Toast.LENGTH_LONG).show();
                    connectCallback.onFailure();
                }
            });

        } catch (MqttException ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Set the callback of the Mqtt client
     * @param callback Callback info
     */
    public void setCallback(final MqttCallback callback) {
        mqttClient.setCallback(callback);
    }

    /**
     * Subscribe to a topic
     * @param topic Topic to subscribe
     */
    public void subscribeToTopic(final String topic, int qos) {
        try {
            mqttClient.subscribe(topic, qos, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Subscribed to " + topic + " successfully");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "Failed to subscribe to " + topic);
                }
            });
        } catch (Exception ex){
            Log.e(TAG, "Error while subscribing to " + topic);
            ex.printStackTrace();
        }
    }
    public void subscribeToTopic(final String topic) {
        this.subscribeToTopic(topic, 1);
    }

    /**
     * Subscribe to a topic with callback
     * @param topic Topic to subscribe
     * @param callback Callback
     */
    public void subscribeToTopic(final String topic, int qos, IMqttActionListener callback) {
        try {
            mqttClient.subscribe(topic, qos, null, callback);
        } catch (MqttException ex){
            Log.e(TAG, "Error while subscribing to " + topic);
            ex.printStackTrace();
        }
    }
    public void subscribeToTopic(final String topic, IMqttActionListener callback) {
        this.subscribeToTopic(topic, 1, callback);
    }

    public void publishMessage(String topic, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(message.getBytes());
            mqttMessage.setRetained(true);
            Log.e(TAG, "Publishing message '" + message + "' to topic '" + topic + "'");
            mqttClient.publish(topic, mqttMessage);
        } catch (MqttException e) {
            Log.e(TAG, "Failed to publish to " + topic);
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            Log.d(TAG, "MqttClient disconnected");
            mqttClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

}
