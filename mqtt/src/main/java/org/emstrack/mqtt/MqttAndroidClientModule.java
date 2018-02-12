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
    private final Context context;

    public MqttAndroidClientModule(Context context, String host, String clientId) {
        this.host = host;
        this.clientId = clientId;
        this.context = context;
    }

    @Provides
    MqttAndroidClient provideMqttAndroidClient() {
        return new MqttAndroidClient(context, host, clientId);
    }

}
