package org.emstrack.ambulance;

import android.app.Application;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.emstrack.mqtt.MqttProfileClient;

import java.util.UUID;

/**
 * Created by mauri on 2/10/2018.
 */

public class AmbulanceApp extends Application {

    final String serverUri = "ssl://cruzroja.ucsd.edu:8883";
    final String clientId = "HospitalAppClient_" + UUID.randomUUID().toString();

    MqttAndroidClient androidClient;
    MqttProfileClient client;

    public MqttProfileClient getProfileClient() {
        // lazy initialization
        if (client == null) {

            androidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
            client = new MqttProfileClient(androidClient);

        }
        return client;
    }

}
