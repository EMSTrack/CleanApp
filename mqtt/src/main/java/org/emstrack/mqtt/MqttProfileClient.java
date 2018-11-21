package org.emstrack.mqtt;

/**
 * Created by mauricio on 2/7/18.
 */

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;
import android.os.Handler;
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

    class TimerRunnable implements Runnable {
        private String topic;

        public TimerRunnable(String topic) {
            this.topic = topic;
        }

        @Override
        public void run() {
            Log.d(TAG, "TIMER RAN OUT! TIMER RAN OUT! Topic: " + topic);

            // reset timer
            timerHandler.postDelayed(this, 70000);
            Log.d(TAG, "RESETTING TIMER! RESETTING TIMER! Topic: " + topic);
        }
    }

    private static final String TAG = MqttProfileClient.class.getSimpleName();

    private final String connectTopic = "user/%1$s/client/%2$s/status";

    private String username;
    private Profile profile;
    private Settings settings;

    private final MqttAndroidClient mqttClient;

    private Map<String,CallbackTuple> subscribedTopics;
    private MqttProfileCallback callback;

    private Handler timerHandler;
    private Map<String, TimerRunnable> timers;

    public MqttProfileClient(MqttAndroidClient mqttClient) {
        this.mqttClient = mqttClient;
        this.subscribedTopics = new HashMap<>();
        this.timerHandler = new Handler();
        this.timers = new HashMap<>();
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

    public void close() {
        if (mqttClient != null) {
            mqttClient.unregisterResources();
            // mqttClient.close();
        }
    }

    public String getClientId() { return mqttClient.getClientId(); }

    public String getServerURI() { return mqttClient.getServerURI(); }

    public void disconnect() throws MqttException { disconnect(null); }

    public void disconnect(final MqttProfileCallback disconnectCallback) throws MqttException {

        // if connected, disconnect
        if (isConnected()) {

            try {

                // Publish to connect topic
                final String topic =
                        String.format(connectTopic,
                                username, mqttClient.getClientId());
                MqttProfileClient.this.publish(topic, "offline", 2, true);

            } catch (MqttException e) {
                Log.e(TAG, "Could not publish client information.");
            }

            // try to disconnect
            mqttClient.disconnect(null,
                    new IMqttActionListener() {

                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {

                            Log.d(TAG, "Successfully disconnected from broker.");
                            setUsername("");

                            // Forward callback
                            if (disconnectCallback != null)
                                disconnectCallback.onSuccess();
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                            Log.d(TAG, "Failed to disconnect to broker failed");
                            Log.e(TAG, exception.getMessage());

                            // Forward callback
                            if (disconnectCallback != null)
                                disconnectCallback.onFailure(exception);
                        }
                    });

        } else {

            // Make sure callback is called
            if (disconnectCallback != null)
                disconnectCallback.onSuccess();

        }

        subscribedTopics = new HashMap<>();
    }

    public boolean isConnected() {
        return mqttClient.isConnected();
    }

    public void setCallback(MqttProfileCallback callback) {
        this.callback = callback;
    }

    public void callOnReconnect() {
        if (callback != null) {
            callback.onReconnect();
        }
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
        Log.d(TAG,String.format("Published '%1$s' to '%2$s'",payload, topic));

        // create a timer to run every 70ish seconds if one for the topic doesnt already exist
        if (!timers.containsKey(topic)) {
            TimerRunnable timerRunnable = new TimerRunnable(topic);
            timerHandler.postDelayed(timerRunnable, 70000);
            timers.put(topic, timerRunnable);
            Log.d(TAG, "SCHEDULED TIMER! SCHEDULED TIMER! Topic: " + topic);
        } else {
            TimerRunnable timerRunnable = timers.get(topic);
            timerHandler.removeCallbacks(timerRunnable);
            timerHandler.postDelayed(timerRunnable, 70000);
            Log.d(TAG,"RESETTING TIMER! RESETTING TIMER! Topic: " + topic);
        }
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

        // remove timer for this topic
        TimerRunnable timerRunnable = timers.get(topic);
        timerHandler.removeCallbacks(timerRunnable);
        timers.remove(topic);
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

        // set will
        final String topic =
                String.format(connectTopic,
                        username,mqttClient.getClientId());
        mqttConnectOptions.setWill(topic, "disconnected".getBytes(), 2, true);

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
                        if (connectCallback != null)
                            connectCallback.onSuccess();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                        Log.d(TAG, "Connection to broker failed");

                        // Forward callback
                        if (connectCallback != null)
                            connectCallback.onFailure(exception);

                    }
                });

    }

    @Override
    public void connectComplete(final boolean reconnect, String serverURI) {

        // TODO: Handle reconnection properly
        if (reconnect) {
            Log.d(TAG, "Reconnected to broker, calling reconnect.");
            callOnReconnect();
        } else
            Log.d(TAG, "Connected to broker");

        // Publish online
        try {

            // publish online to connectTopic
            MqttProfileClient.this.publish(
                    String.format(connectTopic, username, mqttClient.getClientId()),
                    "online", 2, true);

        } catch (MqttException e) {
            Log.e(TAG,"Could not publish to connect topic");
        }

        try {

            // Subscribe to username/{username}/profile
            subscribe(String.format("user/%1$s/profile", username), 2, new MqttProfileMessageCallback() {

                @Override
                public void messageArrived(String topic, MqttMessage message) {

                    if (!reconnect && profile != null) {
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
                        subscribe(String.format("user/%1$s/client/%2$s/error", username, mqttClient.getClientId()), 1);

                    } catch (MqttException e) {
                        callOnFailure(new Exception(String.format("Failed to subscribe to 'user/%1$s/client/%2$s/error",
                                username, mqttClient.getClientId())));
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
            callOnFailure(new Exception(String.format("Failed to subscribe to 'user/%1$s/profile'", username)));
        }

    }

    @Override
    public void connectionLost(Throwable cause) {

        // TODO: Handle reconnection properly
        Log.d(TAG, "Connection to broker lost.");

        // Connection lost is a failure
        callOnFailure(cause);

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

                // received message, reset timer
                TimerRunnable timerRunnable = timers.get(topic);
                timerHandler.removeCallbacks(timerRunnable);
                timerHandler.postDelayed(timerRunnable, 70000);
                Log.d(TAG,"RESETTING TIMER! RESETTING TIMER! Topic: " + topic);

                return;
            }

        }

        // Report failure to handle topic
        callOnFailure(new MishandledTopicException(topic));

    }

}
