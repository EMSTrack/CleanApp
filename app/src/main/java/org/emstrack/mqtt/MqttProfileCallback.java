package org.emstrack.mqtt;

/**
 * Created by mauricio on 2/7/18.
 */

import java.util.regex.Pattern;

import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;

import org.emstrack.models.Profile;
import org.emstrack.models.Settings;
import org.emstrack.mqtt.MqttClient;
import org.emstrack.mqtt.MqttCallback;

import android.content.Context;

public class MqttProfileCallback extends MqttCallback  {

    // regex for parsing message topics
    private static final Pattern profilePattern = Pattern.compile("user/[\\w]+/profile");
    private static final Pattern settingsPattern = Pattern.compile("settings");
    private static final Pattern errorPattern = Pattern.compile("user/[\\w]+/error");
    private static final Pattern hospitalMetadataPattern = Pattern.compile("hospital/[\\w]+/metadata");
    private static final Pattern hospitalEquipmentPattern = Pattern.compile("hospital/[\\w]+/equipment/[\\w]+/data");

    private static final String TAG = "MqttProfileCallback";

    private String username;
    private Profile profile;
    private Settings settings;

    /*
     * MQTTProfileCallback is a thread-safe singleton
     * see: https://www.javaworld.com/article/2073352/core-java/simply-singleton.html
     */

    /**
     * Returns existing instance or creates if none exist
     * @param username Username
     * @return Existing or new instance of the callback handler
     */
    public static synchronized MqttProfileCallback getInstance(Context context, String username) {
        MqttProfileCallback instance = getInstance(context);
        instance.username = username;
        return instance;
    }

    public static synchronized MqttProfileCallback getInstance(Context context) {
        return MqttProfileCallback.getInstance(context, null);
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {

         // TODO: Handle reconnection properly
        if (reconnect)
            Log.d(TAG, "Reconnected to broker");
        else
            Log.d(TAG, "Connected to broker");

        // Clear current profile
        profile = null;

        // TODO: Make sure username is not null, maybe at the instantiation

        // Subscribe to username/{username}/profile
        Log.d(TAG, "Subscribing to user/" + username + "/profile");
        //subscribeToTopic("user/" + username + "/profile");

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
        Log.d(TAG, "Message received: '" + json + "'");

        // Creat Gson parser
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        Gson gson = gsonBuilder.create();

        if (profile == null) {

            // Just connected, no profile
            if (profilePattern.matcher(topic).matches()) {

                // Parse profile
                profile = gson.fromJson(json, Profile.class);
                Log.d(TAG, "Parsed profile: " + profile);

                // Error parsing profile
                if (profile == null) {
                    Log.d(TAG, "Error parsing array");
                    throw new Exception("Could not parse profile");
                }

                // Subscribe to error and settings
                Log.d(TAG, "Subscribing to user/" + username + "/error");
                // subscribeToTopic("user/" + username + "/error");
                //subscribeToTopic("settings");

            } else {
                throw new Exception("Expected profile, got " + topic);
            }

        } else {

            // Already connected
            if (errorPattern.matcher(topic).matches()) {

                // Parse error

            } else if (settingsPattern.matcher(topic).matches()) {

                // Parse settings

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
