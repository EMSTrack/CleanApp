package org.emstrack.hospital;

import android.app.Application;

import org.emstrack.mqtt.MqttProfileClient;

/**
 * Created by mauri on 2/10/2018.
 */

public class HospitalApp extends Application {

    MqttProfileClient client;

    public MqttProfileClient getClient() {
        return client;
    }
}
