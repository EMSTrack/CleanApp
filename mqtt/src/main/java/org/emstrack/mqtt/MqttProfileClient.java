package org.emstrack.mqtt;

/**
 * Created by mauricio on 2/7/18.
 */

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
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

public class MqttProfileClient implements MqttCallbackExtended {

    class CallbackTuple {
        private final MqttProfileMessageCallback callback;
        private final Pattern pattern;

        public CallbackTuple(Pattern pattern, MqttProfileMessageCallback callback) {
            this.pattern = pattern;
            this.callback = callback;
        }

        public MqttProfileMessageCallback getCallback() {
            return callback;
        }

        public Pattern getPattern() {
            return pattern;
        }
    }

   private static final String TAG = "MqttProfileClient";

    private String username;
    private Profile profile;
    private Settings settings;

    private final MqttAndroidClient mqttClient;

    private Map<String,CallbackTuple> subscribedTopics;
    private MqttProfileCallback callback;

    public MqttProfileClient(MqttAndroidClient mqttClient) {
        this.mqttClient = mqttClient;
        this.subscribedTopics = new HashMap<>();
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

    public void disconnect() throws MqttException {
        // if connected, disconnect
        if (isConnected()) {
            mqttClient.disconnect();
        }
        subscribedTopics = new HashMap<>();
    }

    public boolean isConnected() {
        return mqttClient.isConnected();
    }

    public void setCallback(MqttProfileCallback callback) {
        this.callback = callback;
    }

    public void callOnSuccess() {
        if (callback != null) {
            callback.onSuccess();
        }
    }

    public void callOnFailure(Throwable exception) {
        if (callback != null) {
            callback.onFailure(exception);
        } else {
            Log.d(TAG, "Unhandled exception '" + exception + "'");
        }
    }

    public void publish(String topic, String payload, int qos, boolean retained) throws MqttException {
        mqttClient.publish(topic, payload.getBytes(), qos, retained);
    }

    public void subscribe(String topic, int qos) throws MqttException {
        subscribe(topic, qos, null);
    }

    public void subscribe(String topic, int qos, MqttProfileMessageCallback callback) throws MqttException {

        // Subscribe to topic
        Log.d(TAG, "Subscribing to '" + topic + "'");
        mqttClient.subscribe(topic, qos);

        // Register callback
        if (callback != null) {

            // Parse topic
            String regex = topic;
            if (regex.indexOf('+') >= 0) {
                regex = regex.replace("+", "[^/]+");
            }
            if (regex.indexOf('#') == regex.length() - 1) {
                regex = regex.replace("#", "[a-zA-Z0-9_/ ]+");
            }
            // Invalid topic
            if (regex.indexOf('#') >= 0) {
                throw new MqttException(new Exception("Invalid topic '" + topic + "'"));
            }

            Pattern pattern = Pattern.compile(regex);
            Log.d(TAG, "Registering callback '" + topic + " -> " + pattern + "'");
            subscribedTopics.put(topic,
                    new CallbackTuple(pattern, callback));

        } else {
            Log.d(TAG, "No callback was registered");
        }

    }

    public void unsubscribe(String topic) throws MqttException {

        // Unsubscribe
        mqttClient.unsubscribe(topic);

        // Remove callback
        subscribedTopics.remove(topic);

    }

    /**
     * Connect the client to the broker with a username and password.
     * To reset the callback, use the setCallback function instead.
     * @param username Username
     * @param password Password
     */
    public void connect(final String username, final String password,
                        final MqttProfileCallback connectCallback) throws MqttException {

        // if connected, disconnect first
        if (isConnected()) {
            disconnect();
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

                        // Forward callback
                        if (connectCallback != null) connectCallback.onSuccess();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                        Log.d(TAG, "Connection to broker failed");
                        Log.e(TAG, exception.getMessage());

                        // Forward callback
                        if (connectCallback != null) connectCallback.onFailure(exception);
                    }
                });

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
            subscribe("user/" + username + "/profile", 2, new MqttProfileMessageCallback() {

                @Override
                public void messageArrived(String topic, MqttMessage message) {

                    if (profile != null) {
                        // Should never happen
                        callOnFailure(new Exception("Profile already exists!"));
                        return;
                    }

                    // Parse profile
                    GsonBuilder gsonBuilder = new GsonBuilder();
                    gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                    Gson gson = gsonBuilder.create();

                    profile = gson.fromJson(new String(message.getPayload()), Profile.class);

                    // Error parsing profile
                    if (profile == null) {
                        callOnFailure(new Exception("Could not parse profile"));
                        return;
                    }
                    Log.d(TAG, "Parsed profile: " + profile);

                    // Unsubscribe from profile
                    try {
                        unsubscribe(topic);
                    } catch (MqttException e) {
                        callOnFailure(new Exception("Failed to unsubscribe to '" + topic + "'"));
                        return;
                    }

                    try {

                        // Subscribe to error
                        subscribe("user/" + username + "/error", 1);

                    } catch (MqttException e) {
                        callOnFailure(new Exception("Failed to subscribe to 'user/" + username + "/error'"));
                        return;
                    }

                    try {

                        // Subscribe to settings
                        subscribe("settings", 1, new MqttProfileMessageCallback() {

                            @Override
                            public void messageArrived(String topic, MqttMessage message) {

                                // Parse settings
                                GsonBuilder gsonBuilder = new GsonBuilder();
                                gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                                Gson gson = gsonBuilder.create();

                                settings = gson.fromJson(new String(message.getPayload()), Settings.class);

                                if (settings == null) {
                                    callOnFailure(new Exception("Could not parse settings"));
                                    return;
                                }
                                Log.d(TAG, "Got settings = " + settings);

                                // Unsubscribe from settings
                                try {
                                    unsubscribe(topic);
                                } catch (MqttException e) {
                                    callOnFailure(new Exception("Failed to unsubscribe to '" + topic + "'"));
                                    return;
                                }

                                // Call on success
                                callOnSuccess();

                            }

                        });

                    } catch (MqttException e) {
                        callOnFailure(new Exception("Failed to subscribe to 'settings'"));
                    }

                }

            });

        } catch (MqttException e) {
            callOnFailure(new Exception("Failed to subscribe to 'user/" + username + "/profile'"));
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
    public void messageArrived(String topic, MqttMessage message) throws MqttException {

        String json = new String(message.getPayload());
        Log.d(TAG, "Message received: topic = '" + topic + "', message = '" + json + "'");

        // Does message have registered callback?
        for (Map.Entry<String, CallbackTuple> entry: subscribedTopics.entrySet()) {

            Pattern pattern = entry.getValue().getPattern();
            MqttProfileMessageCallback callback = entry.getValue().getCallback();

            Matcher m = pattern.matcher(topic);
            if (m.matches()) {
                // Perform callback
                Log.d(TAG, "Found match. Calling back...");
                callback.messageArrived(topic, message);
                return;
            }

        }

        // Report failure to handle topic
        callOnFailure(new Exception("Mishandled topic '" + topic + "'"));

    }

}
