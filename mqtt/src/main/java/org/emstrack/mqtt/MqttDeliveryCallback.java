package org.emstrack.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;

public interface MqttDeliveryCallback {

    void onSuccess();

}
