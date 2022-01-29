package org.emstrack.mqtt;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
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
        // Context appContext = InstrumentationRegistry.getTargetContext();
        Context appContext = ApplicationProvider.getApplicationContext();

        Thread.sleep(1000);

        assertEquals("org.emstrack.mqtt.test", appContext.getPackageName());

        final String serverUri = appContext.getString(R.string.mqttServer);
        final String clientId = "TestClient_" + UUID.randomUUID().toString();
        final String username = appContext.getString(R.string.mqttUser);
        final String password = appContext.getString(R.string.mqttPassword);

        MqttAsyncClient client = new MqttAsyncClient(serverUri, clientId, null);
        final MqttProfileClient profileClient = new MqttProfileClient(client);

        // Test login
        profileClient.setCallback(new MqttProfileCallback() {
            @Override
            public void onReconnect() {
                fail();
            }

            @Override
            public void onSuccess() {
                assertTrue(true);
            }

            @Override
            public void onFailure(Throwable exception) {
                Log.d(TAG, "onFailure: " + exception);
                fail();
            }

        });

        // Test login
        profileClient.connect(username, password, new MqttProfileCallback() {
            @Override
            public void onReconnect() {
                fail();
            }

            @Override
            public void onSuccess() {

                assertTrue(profileClient.isConnected());

            }

            @Override
            public void onFailure(Throwable exception) {
                fail();
            }
        });

        /*
        // Test settings
        assertEquals("Tijuana", profileClient.getSettings().getDefaults().getCity());
        assertEquals("BCN", profileClient.getSettings().getDefaults().getState());
        assertEquals("MX", profileClient.getSettings().getDefaults().getCountry());

        // Test hospitals
        List<HospitalPermission> hospitalPermissions = profileClient.getProfile().getHospitals();
        final int numberOfHospitals[] = {hospitalPermissions.size()};
        for (HospitalPermission hospitalPermission: hospitalPermissions) {

            final int hospitalId = hospitalPermission.getHospitalId();

            try {

                // Start retrieving data
                profileClient.subscribe("hospital/" + hospitalId + "/data",
                    1, new MqttProfileMessageCallback() {

                            @Override
                            public void messageArrived(String topic, MqttMessage message) {

                                try {

                                    // Unsubscribe to hospital
                                    profileClient.unsubscribe("hospital/" + hospitalId + "/data");

                                } catch (MqttException exception) {

                                    Log.d(TAG, "Could not unsubscribe to 'hospital/" + hospitalId + "/data'");
                                    return;
                                }

                                // Parse to hospital metadata
                                GsonBuilder gsonBuilder = new GsonBuilder();
                                gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                                Gson gson = gsonBuilder.create();

                                // / Found item in the ambulance equipments object
                                Hospital hospital = gson
                                        .fromJson(message.toString(), Hospital.class);

                                // Got one hospital
                                numberOfHospitals[0]--;

                            }
                        });

            } catch (MqttException e) {
                Log.d(TAG, "Could not subscribe to hospitals");
            }
        }

        // Wait for all hospitals to come
        int numberOfAttempts = 10;
        while (numberOfHospitals[0] > 0 && numberOfAttempts-- > 0) {
            Thread.sleep(1000);
        }
        assertEquals(0, numberOfHospitals[0]);

        // Test ambulances
        List<AmbulancePermission> ambulancePermissions = profileClient.getProfile().getAmbulances();
        final int numberOfAmbulances[] = {ambulancePermissions.size()};
        for (AmbulancePermission ambulancePermission: ambulancePermissions) {

            final int ambulanceId = ambulancePermission.getAmbulanceId();

            try {

                // Start retrieving data
                profileClient.subscribe("ambulance/" + ambulanceId + "/data",
                        1, new MqttProfileMessageCallback() {

                            @Override
                            public void messageArrived(String topic, MqttMessage message) {

                                try {

                                    // Unsubscribe to ambulance
                                    profileClient.unsubscribe("ambulance/" + ambulanceId + "/data");

                                } catch (MqttException exception) {

                                    Log.d(TAG, "Could not unsubscribe to 'ambulance/" + ambulanceId + "/data'");
                                    return;
                                }

                                // Parse to ambulance metadata
                                GsonBuilder gsonBuilder = new GsonBuilder();
                                gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                                Gson gson = gsonBuilder.create();

                                // / Found item in the ambulance equipments object
                                Ambulance ambulance = gson
                                        .fromJson(message.toString(), Ambulance.class);

                                // Got one ambulance
                                numberOfAmbulances[0]--;

                            }
                        });

            } catch (MqttException e) {
                Log.d(TAG, "Could not subscribe to ambulances");
            }
        }

        // Wait for all ambulances to come
        numberOfAttempts = 10;
        while (numberOfAmbulances[0] > 0 && numberOfAttempts-- > 0) {
            Thread.sleep(1000);
        }
        assertEquals(0, numberOfAmbulances[0]);


        */

        // Test logout
        Thread.sleep(4000);
        profileClient.disconnect();
        Thread.sleep(1000);
        assertFalse(profileClient.isConnected());

    }

}
