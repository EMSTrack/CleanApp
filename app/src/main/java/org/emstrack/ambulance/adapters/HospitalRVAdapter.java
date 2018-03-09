package org.emstrack.ambulance.adapters;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.emstrack.ambulance.AmbulanceApp;
import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.fragments.HospitalEquipmentFragment;
import org.emstrack.models.HospitalEquipment;
import org.emstrack.models.HospitalEquipmentMetadata;
import org.emstrack.models.HospitalPermission;
import org.emstrack.mqtt.MqttProfileClient;
import org.emstrack.mqtt.MqttProfileMessageCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tina on 3/6/18.
 */

public class HospitalRVAdapter extends RecyclerView.Adapter<HospitalRVAdapter.HospitalViewHolder> {

    static class HospitalViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        ImageView hospitalImageIV;
        TextView hospitalNameTV;
        String hospitalName;

        private HospitalViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cv);
            hospitalImageIV = itemView.findViewById(R.id.hospital_image);
            hospitalNameTV = itemView.findViewById(R.id.hospital_name);
        }

        private void setHospitalName(String hospitalName) {
            this.hospitalName = hospitalName;
            hospitalNameTV.setText(hospitalName);
        }

        private void setHospitalImageIV() {
        }

        private void setClickListener(final int hospitalId,
                                      final List<HospitalEquipment> hospitalEquipment) {

            cardView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {

                    Log.e(TAG, "Clicked Hospital: " + hospitalNameTV.getText().toString());

                    String fragmentTag = String.valueOf(hospitalId);
                    FragmentManager fragmentManager = ((MainActivity) cardView.getContext())
                            .getSupportFragmentManager();
                    Fragment equipmentFragment = fragmentManager.findFragmentByTag(fragmentTag);

                    if (equipmentFragment == null) {

                        Log.e(TAG, "New Fragment");
                        equipmentFragment = new HospitalEquipmentFragment();
                        Bundle arguments = new Bundle();
                        arguments.putParcelableArrayList("hospitalEquipment",
                                (ArrayList<? extends Parcelable>) hospitalEquipment);
                        arguments.putString("hospitalName", hospitalName);
                        equipmentFragment.setArguments(arguments);
                        fragmentManager.beginTransaction()
                                .add(R.id.root, equipmentFragment, fragmentTag)
                                .addToBackStack(fragmentTag)
                                .commit();

                    } else {
                        Log.e(TAG, "Old Fragment");
                        fragmentManager.beginTransaction()
                                .replace(R.id.root, equipmentFragment, fragmentTag)
                                .addToBackStack(fragmentTag)
                                .commit();
                    }
                }
            });
        }
    }

    private static String TAG = HospitalRVAdapter.class.getSimpleName();
    private List<HospitalPermission> hospitals;
    private Fragment fragment;

    /* Constructor takes fragment */
    public HospitalRVAdapter(List<HospitalPermission> hospitals, Fragment fragment) {
        this.hospitals = hospitals;
        this.fragment = fragment;
    }

    @Override
    public HospitalViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rv_hospital_item, parent, false);
        return new HospitalViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final HospitalViewHolder holder, int position) {

        // Which hospital?
        final HospitalPermission hospital = hospitals.get(position);
        final int hospitalId = hospital.getHospitalId();

        // Retrieve equipment metadata
        final MqttProfileClient profileClient = ((AmbulanceApp) fragment.getActivity().getApplication()).getProfileClient();

        try {

            // Start retrieving data
            profileClient.subscribe("hospital/" + hospitalId + "/metadata",
                    1, new MqttProfileMessageCallback() {

                        @Override
                        public void messageArrived(String topic, MqttMessage message) {

                            try {

                                // Unsubscribe to metadata
                                profileClient.unsubscribe("hospital/" + hospitalId + "/metadata");

                            } catch (MqttException exception) {

                                Log.d(TAG, "Could not unsubscribe to 'hospital/" + hospitalId + "/metadata'");
                                return;
                            }

                            // Parse to hospital metadata
                            GsonBuilder gsonBuilder = new GsonBuilder();
                            gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                            Gson gson = gsonBuilder.create();

                            // / Found item in the ambulance equipments object
                            final HospitalEquipmentMetadata[] hospitalEquipmentMetadataList = gson
                                    .fromJson(message.toString(), HospitalEquipmentMetadata[].class);

                            // Retrieve equipment list
                            final List<HospitalEquipment> hospitalEquipmentList = new ArrayList<>();
                            try {

                                // Start retrieving equipment data
                                profileClient.subscribe("hospital/" + hospitalId + "/equipment/+/data",
                                        1, new MqttProfileMessageCallback() {

                                            @Override
                                            public void messageArrived(String topic, MqttMessage message) {

                                                // Parse to equipment data
                                                GsonBuilder gsonBuilder = new GsonBuilder();
                                                gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                                                Gson gson = gsonBuilder.create();

                                                // / Found item in the ambulance equipments object
                                                HospitalEquipment equipment = gson
                                                        .fromJson(new String(message.getPayload()),
                                                                HospitalEquipment.class);

                                                // Add to equipment list
                                                hospitalEquipmentList.add(equipment);

                                                // Done yet?
                                                if (hospitalEquipmentList.size() == hospitalEquipmentMetadataList.length) {

                                                    // done!
                                                    try {

                                                        // Unsubscribe to all equipment data
                                                        profileClient.unsubscribe("hospital/" + hospitalId + "/equipment/+/data");

                                                    } catch (MqttException exception) {

                                                        Log.d(TAG, "Could not unsubscribe to 'hospital/" + hospitalId + "/equipment/+/data'");

                                                    }

                                                    // Initialize holder
                                                    holder.setHospitalName(hospital.getHospitalName());
                                                    holder.setClickListener(hospital.getHospitalId(),
                                                            hospitalEquipmentList);

                                                }
                                            }
                                        });

                            } catch (MqttException e) {
                                Log.d(TAG, "Could not subscribe to hospital equipment");
                            }

                        }

                    });

        } catch (MqttException e) {
            Log.d(TAG, "Could not subscribe to hospital metadata");
        }
    }

    @Override
    public int getItemCount() {

        return hospitals.size();

    }

}
