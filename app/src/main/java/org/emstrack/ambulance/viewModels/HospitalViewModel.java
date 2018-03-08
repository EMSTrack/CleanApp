package org.emstrack.ambulance.viewModels;

import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.emstrack.models.HospitalEquipment;
import org.emstrack.models.HospitalEquipmentMetadata;
import org.emstrack.models.Hospital;
import org.emstrack.mqtt.MqttProfileClient;
import org.emstrack.mqtt.MqttProfileMessageCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jessicakwok on 2/27/18.
 */

public class HospitalViewModel {

    private static final String TAG = HospitalViewModel.class.getSimpleName();
    private MqttProfileClient profileClient;

    public HospitalViewModel(MqttProfileClient profileClient){
        this.profileClient = profileClient;
    }

    public void getHospitalMetadata() {
        final List<Hospital> hospitals = profileClient.getProfile().getHospitals();
        Log.d("hospitals1234", hospitals.toString());
        for (final Hospital hospital : hospitals) {
            try {
                // Start retrieving data
                profileClient.subscribe("hospital/" + hospital.getHospitalId() + "/metadata",
                        1, new MqttProfileMessageCallback() {

                            @Override
                            public void messageArrived(String topic, MqttMessage message) {

                                try {

                                    // Unsubscribe to metadata
                                    profileClient.unsubscribe("hospital/" + hospital.getHospitalId() + "/metadata");

                                } catch (MqttException exception) {

                                    Log.d(TAG, "Could not unsubscribe to 'hospital/" + hospital.getHospitalId() + "/metadata'");
                                    return;
                                }

                                // Parse to ambulance metadata
                                GsonBuilder gsonBuilder = new GsonBuilder();
                                gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                                Gson gson = gsonBuilder.create();

                                // / Found item in the ambulance equipments object
                                HospitalEquipmentMetadata[] hospitalEquipmentMetadataList = gson
                                        .fromJson(message.toString(), HospitalEquipmentMetadata[].class);
                                Log.d("equip metadata", hospitalEquipmentMetadataList.toString());

                                getEquipmentData(hospital, hospitalEquipmentMetadataList);
                            }

                        });

            } catch (MqttException e) {
                Log.d(TAG, "Could not subscribe to hospital metadata");
            }
        }
    }

    private void getEquipmentData(final Hospital hospital, HospitalEquipmentMetadata[] hospitalEquipmentMetadataList) {
        final ArrayList<HospitalEquipment> hospitalEquipmentList = new ArrayList<>();
        for (final HospitalEquipmentMetadata hospitalMetadata : hospitalEquipmentMetadataList) {
            try {
                for (final HospitalEquipmentMetadata equipmentMetadata : hospitalEquipmentMetadataList)
                // Start retrieving data
                profileClient.subscribe("hospital/" + hospital.getHospitalId() + "/equipment/" + equipmentMetadata.getName() + "/data",
                        1, new MqttProfileMessageCallback() {

                            @Override
                            public void messageArrived(String topic, MqttMessage message) {

                                try {

                                    // Unsubscribe to metadata
                                    profileClient.unsubscribe("hospital/" + hospital.getHospitalId() +
                                            "/equipment/" + equipmentMetadata.getName() + "/data");

                                } catch (MqttException exception) {

                                    Log.d(TAG, "Could not unsubscribe to 'hospital/" + hospital.getHospitalId() +
                                            "/equipment/" + equipmentMetadata.getName() + "/data'");
                                    return;
                                }

                                // Parse to equipment data
                                GsonBuilder gsonBuilder = new GsonBuilder();
                                gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                                Gson gson = gsonBuilder.create();

                                // / Found item in the ambulance equipments object
                                HospitalEquipment equipment = gson
                                        .fromJson(new String(message.getPayload()),
                                                HospitalEquipment.class);
                                hospitalEquipmentList.add(equipment);
                                Log.d("hospitalequip", equipment.getEquipmentName());
                            }

                        });

            } catch (MqttException e) {
                Log.d(TAG, "Could not subscribe to hospital metadata");
            }
        }
        hospital.setHospitalEquipment(hospitalEquipmentList);
    }
}