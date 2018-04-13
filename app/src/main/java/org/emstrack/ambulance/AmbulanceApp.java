package org.emstrack.ambulance;

import android.app.Application;
import android.content.Intent;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.Ambulance;
import org.emstrack.mqtt.MqttProfileClient;

import java.util.UUID;

import static java.security.AccessController.getContext;

/**
 * Created by mauricio on 2/10/2018.
 */

public class AmbulanceApp extends Application {

}
