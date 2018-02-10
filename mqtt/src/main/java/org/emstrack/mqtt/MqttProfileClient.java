package org.emstrack.mqtt;

/**
 * Created by mauricio on 2/7/18.
 */

import java.util.regex.Pattern;

import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.paho.android.service.MqttAndroidClient;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;

import org.emstrack.models.Profile;
import org.emstrack.models.Settings;

import javax.inject.Inject;

public class MqttProfileClient implements MqttCallbackExtended {

    // regex for parsing message topics
    private static final Pattern profilePattern = Pattern.compile("user/[\\w]+/profile");
    private static final Pattern errorPattern = Pattern.compile("user/[\\w]+/error");
    private static final Pattern hospitalMetadataPattern = Pattern.compile("hospital/[\\w]+/metadata");
    private static final Pattern hospitalEquipmentPattern = Pattern.compile("hospital/[\\w]+/equipment/[\\w]+/data");

    private static final String TAG = "MqttProfileClient";

    private String username;
    private Profile profile;
    private Settings settings;

    private MqttAndroidClient mqttClient;

    @Inject
    public MqttProfileClient(MqttAndroidClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    public void setUsername(String username) {
        this.username = username;
        profile = null;
        settings = null;
    }

    public String getUsername() {
        return username;
    }

    public Profile getProfile() {
        return profile;
    }

    public Settings getSettings() {
        return settings;
    }

    /**
     * Connect the client to the broker with a username and password.
     * To reset the callback, use the setCallback function instead.
     * @param username Username
     * @param password Password
     */
    public void connect(final String username, String password) throws MqttException {

        // if connected, disconnect first
        if (mqttClient.isConnected()) {
            mqttClient.disconnect();
        }

        // set new username
        setUsername(username);

        // Set callback to handle connectComplete
        mqttClient.setCallback(this);

        // Set client options
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(password.toCharArray());

        mqttClient.connect(mqttConnectOptions,
                null,
                new IMqttActionListener() {

                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG, "Connection to broker successful as clientId = " + mqttClient.getClientId());
                        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                        disconnectedBufferOptions.setBufferEnabled(true);
                        disconnectedBufferOptions.setBufferSize(100);
                        disconnectedBufferOptions.setPersistBuffer(false);
                        disconnectedBufferOptions.setDeleteOldestMessages(false);
                        mqttClient.setBufferOpts(disconnectedBufferOptions);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.d(TAG, "Connection to broker failed");
                        Log.e(TAG, exception.getMessage());
                    }
                });

    }

    public void disconnect() throws MqttException {
        mqttClient.disconnect();
    }

    public boolean isConnected() {
        return mqttClient.isConnected();
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {

         // TODO: Handle reconnection properly
        if (reconnect)
            Log.d(TAG, "Reconnected to broker");
        else
            Log.d(TAG, "Connected to broker");

        try {

            // Subscribe to username/{username}/profile
            Log.d(TAG, "Subscribing to 'user/" + username + "/profile'");
            mqttClient.subscribe("user/" + username + "/profile", 2);

        } catch (MqttException e) {
            Log.d(TAG, "Failed to subscribe to 'user/" + username + "/profile'");
        }

    }

    @Override
    public void connectionLost(Throwable cause) {
        // TODO: Handle reconnection properly
        Log.d(TAG, "Connection to broker lost");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(TAG, "Message sent successfully");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

        String json = new String(message.getPayload());
        Log.d(TAG, "Message received: topic = '" + topic + "', message = '" + json + "'");

        // Create Gson parser
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        Gson gson = gsonBuilder.create();

        if (profile == null) {

            Log.d(TAG, "Null profile");

            // Just connected, no profile
            if (profilePattern.matcher(topic).matches()) {

                // Parse profile
                profile = gson.fromJson(json, Profile.class);

                // Error parsing profile
                if (profile == null) {
                    throw new Exception("Could not parse profile");
                }
                Log.d(TAG, "Parsed profile: " + profile);

                // Unsubscribe from profile
                Log.d(TAG, "Unsubscribing from 'user/" + username + "/profile'");
                mqttClient.unsubscribe("user/" + username + "/profile");

                // Subscribe to error and settings
                Log.d(TAG, "Subscribing to 'user/" + username + "/error' and 'settings'");
                mqttClient.subscribe("user/" + username + "/error",1);
                mqttClient.subscribe("settings",1);

            } else {
                throw new Exception("Expected profile, got " + topic);
            }

        } else {

            // Already connected
            Log.d(TAG, "Got topic '" + topic + "'");

            if (topic.equals("settings")) {

                // Parse settings
                settings = gson.fromJson(json, Settings.class);
                if (settings == null) {
                    throw new Exception("Could not parse settings");
                }
                Log.d(TAG,"Got settings = " + settings);

                // Unsubscribe from settings
                Log.d(TAG, "Unsubscribing from 'settings'");
                mqttClient.unsubscribe("settings");

            } else if (errorPattern.matcher(topic).matches()) {

                // Parse error

            } else if (hospitalMetadataPattern.matcher(topic).matches()) {

                // Parse hospital metadata

            } else if (hospitalEquipmentPattern.matcher(topic).matches()) {

                // Parse hospital equipment
            } else {

                throw new Exception("Unexpected topic " + topic);

            }

        }

    }

}
