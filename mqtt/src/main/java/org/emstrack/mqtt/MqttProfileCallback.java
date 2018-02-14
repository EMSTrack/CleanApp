package org.emstrack.mqtt;

/**
 * Created by mauricio on 2/13/18.
 */

public interface MqttProfileCallback {

    void onSuccess();

    void onFailure(Throwable exception);

}
