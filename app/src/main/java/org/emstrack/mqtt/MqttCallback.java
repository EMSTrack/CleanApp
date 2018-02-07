package org.emstrack.mqtt;

/**
 * Created by mauricio on 2/6/18.
 */

import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;

public abstract class MqttCallback implements MqttCallbackExtended {

    private static final String TAG = "MqttCallback";

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        if (reconnect)
            Log.d(TAG, "Reconnected to broker");
        else
            Log.d(TAG, "Connected to broker");
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.d(TAG, "Connection to broker lost");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(TAG, "Message sent successfully");
    }

}
