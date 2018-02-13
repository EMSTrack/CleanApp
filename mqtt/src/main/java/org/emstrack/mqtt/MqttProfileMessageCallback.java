package org.emstrack.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by mauricio on 2/13/18.
 */

public interface MqttProfileMessageCallback {

    void messageArrived(String topic, MqttMessage message);

}
