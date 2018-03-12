package org.emstrack.ambulance.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.emstrack.ambulance.AmbulanceApp;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.HospitalExpandableRecyclerAdapter;
import org.emstrack.ambulance.models.HospitalExpandableGroup;
import org.emstrack.models.Hospital;
import org.emstrack.models.HospitalPermission;
import org.emstrack.mqtt.MqttProfileClient;
import org.emstrack.mqtt.MqttProfileMessageCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is purely meant to demonstrate that the information is able to send
 * to the server. The website is:
 *
 * http://cruzroja.ucsd.edu/ambulances/info/123456
 *
 *
 */
public class HospitalFragment extends Fragment {

    private static final String TAG = HospitalFragment.class.getSimpleName();

    View rootView;
    RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_hospital, container, false);
        recyclerView = rootView.findViewById(R.id.recycler_view);

        // Retrieve hospital data
        final MqttProfileClient profileClient = ((AmbulanceApp) getActivity().getApplication()).getProfileClient();
        final List<HospitalPermission> hospitalPermissions = profileClient.getProfile().getHospitals();

        final List hospitalExpandableGroup = new ArrayList<HospitalExpandableGroup>();
        for (HospitalPermission hospitalPermission : hospitalPermissions) {

            final int hospitalId = hospitalPermission.getHospitalId();

            try {

                // Start retrieving data
                profileClient.subscribe("hospital/" + hospitalId + "/data",
                        1, new MqttProfileMessageCallback() {

                            @Override
                            public void messageArrived(String topic, MqttMessage message) {

                                try {

                                    // Unsubscribe to hospital data
                                    profileClient.unsubscribe("hospital/" + hospitalId + "/data");

                                } catch (MqttException exception) {
                                    Log.d(TAG, "Could not unsubscribe to 'hospital/" + hospitalId + "/data'");
                                    return;
                                }

                                // Parse to hospital metadata
                                GsonBuilder gsonBuilder = new GsonBuilder();
                                gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                                Gson gson = gsonBuilder.create();

                                // / Found hospital
                                final Hospital hospital = gson.fromJson(message.toString(), Hospital.class);
                                hospitalExpandableGroup.add(
                                        new HospitalExpandableGroup(hospital.getName(),
                                                hospital.getHospitalequipmentSet(), hospital));

                                // Done yet?
                                if (hospitalExpandableGroup.size() == hospitalPermissions.size()) {

                                    Log.d(TAG, "Installing HospitalFragment");

                                    // Install fragment
                                    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
                                    HospitalExpandableRecyclerAdapter adapter =
                                            new HospitalExpandableRecyclerAdapter(hospitalExpandableGroup);
                                    recyclerView.setLayoutManager(linearLayoutManager);
                                    recyclerView.setAdapter(adapter);

                                }
                            }
                        });

            } catch (MqttException e) {
                Log.d(TAG, "Could not subscribe to hospital data");
            }

        }

        return rootView;
    }

}