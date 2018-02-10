package org.emstrack.mqtt;

import android.content.Context;
import org.eclipse.paho.android.service.MqttAndroidClient;

import dagger.Module;
import dagger.Provides;

/**
 * Created by mauricio on 2/9/18.
 */

@Module
public class MqttAndroidClientModule {

    private final String host;
    private final String clientId;

    public MqttAndroidClientModule(String host, String clientId) {
        this.host = host;
        this.clientId = clientId;
    }

    @Provides
    MqttAndroidClient provideMqttAndroidClient(Context context) {
        return new MqttAndroidClient(context, host, clientId);
    }

}
