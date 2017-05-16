package com.project.cruzroja.hospital;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by Fabian Choi on 5/12/2017.
 * Singleton that connects to the Mqqt broker.
 */

public class MqttClient {
    private static final String TAG = MqttClient.class.getSimpleName();
    private static MqttClient instance;
    private static Context context;

    private MqttAndroidClient mqttClient;

    private final String serverUri = "ssl://cruzroja.ucsd.edu:8883";
    private final String publishTopic = "exampleAndroidPublishTopic"; // TODO: FILL OUT
    private String clientId = "HospitalAppClient-";

    //private HashMap<Integer, SSLSocketFactory> socketFactoryMap;

    private MqttClient(Context context) {
        MqttClient.context = context;
        //socketFactoryMap = new HashMap<>();
        clientId += System.currentTimeMillis();

        // Initialize the client
        mqttClient = new MqttAndroidClient(context, serverUri, clientId);
        mqttClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if(reconnect) {
                    Log.d(TAG, "Reconnected to broker");
                } else {
                    Log.d(TAG, "Connected to broker");
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "Connection to broker lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(TAG, "Message received: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "Message sent successfully");
            }
        });

        // Set client options
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);

        try {
            //addToHistory("Connecting to " + serverUri);
            mqttClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic("test");
                    Log.d(TAG, "Connection to broker successful as clientId = " + clientId);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "Connection to broker failed");
                    Log.e(TAG, exception.getMessage());
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    /*public SSLSocketFactory getSocketFactory(int certificateId, String certificatePassword) {
        // Check if certificate exists
        SSLSocketFactory result = socketFactoryMap.get(certificateId);

        // Not cached
        if (result == null && context != null) {
            try {
                KeyStore keystoreTrust = KeyStore.getInstance("BKS");
                keystoreTrust.load(context.getResources().openRawResource(certificateId),
                        certificatePassword.toCharArray());

                TrustManagerFactory trustManagerFactory =
                        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keystoreTrust);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

                result = sslContext.getSocketFactory();

                socketFactoryMap.put(certificateId, result); // Cache for reuse
            }
            catch (Exception ex) {
                // log exception
            }
        }
        return result;
    }*/

    public void subscribeToTopic(final String topic){
        try {
            mqttClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Subscribed to " + topic + " successfully");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "Failed to subscribe to " + topic);
                }
            });
        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    public static synchronized MqttClient getInstance(Context context) {
        if(instance == null) {
            instance = new MqttClient(context);
        }
        return instance;
    }
}
