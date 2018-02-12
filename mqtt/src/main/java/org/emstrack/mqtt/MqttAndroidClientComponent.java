package org.emstrack.mqtt;

import android.content.Context;

import dagger.Component;

/**
 * Created by mauri on 2/10/2018.
 */

@Component(modules = {MqttAndroidClientModule.class})
public interface MqttAndroidClientComponent {
    MqttAndroidClientComponent getMqttAndroidClientComponent();
}
