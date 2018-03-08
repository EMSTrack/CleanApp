package org.emstrack.mqtt;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MqttInstrumentedTest {

    private static final String TAG = "MqttProfileClientInstrumentedTest";

    @Test
    public void testMqttProfileClient() throws Exception {

        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        Thread.sleep(1000);

        assertEquals("org.emstrack.mqtt.test", appContext.getPackageName());

        final String serverUri = "ssl://cruzroja.ucsd.edu:8883";
        final String clientId = "TestClient_" + UUID.randomUUID().toString();
        final String username = "admin";
        final String password = "cruzrojaadmin";

        MqttAndroidClient client = new MqttAndroidClient(appContext, serverUri, clientId);
        MqttProfileClient profileClient = new MqttProfileClient(client);

        // Test login
        profileClient.setCallback(new MqttProfileCallback() {
            @Override
            public void onSuccess() {
                assertEquals(true, true);
            }

            @Override
            public void onFailure(Throwable exception) {
                Log.d(TAG, "onFailure: " + exception);
                assertEquals(true, false);
            }
        });

        // Test login
        profileClient.connect(username, password, new MqttProfileCallback() {
            @Override
            public void onSuccess() {
                assertEquals(true, true);
            }

            @Override
            public void onFailure(Throwable exception) {
                assertEquals(true, false);
            }
        });
        Thread.sleep(2000);
        assertEquals(true, profileClient.isConnected());

        // Test settings
        assertEquals("Tijuana", profileClient.getSettings().getDefaults().getCity());
        assertEquals("BC", profileClient.getSettings().getDefaults().getState());
        assertEquals("MX", profileClient.getSettings().getDefaults().getCountry());

        // Test logout
        profileClient.disconnect();
        Thread.sleep(1000);
        assertEquals(false, profileClient.isConnected());

    }

}
