package org.emstrack.ambulance.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Toast;

import org.emstrack.ambulance.AmbulanceApp;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.HospitalAdapter;
import org.emstrack.ambulance.adapters.HospitalRVAdapter;
import org.emstrack.models.HospitalPermission;
import org.emstrack.mqtt.MqttProfileClient;

import java.util.List;

import static org.emstrack.ambulance.FeatureFlags.OLD_HOSPITAL_UI;

/**
 * This class is purely meant to demonstrate that the information is able to send
 * to the server. The website is:
 *
 * http://cruzroja.ucsd.edu/ambulances/info/123456
 *
 *
 */
public class HospitalFragment extends Fragment {

    View rootView;
    ListView listView;

    private ExpandableListView hospitalExpandableList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_hospital, container, false);
        final MqttProfileClient profileClient = ((AmbulanceApp) getActivity().getApplication()).getProfileClient();
        final List<HospitalPermission> hospitals = profileClient.getProfile().getHospitals();

        RecyclerView recyclerView = rootView.findViewById(R.id.rv);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        HospitalRVAdapter adapter = new HospitalRVAdapter(hospitals);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);

        return rootView;
    }

    public void onClick(View v) {
        if(v == listView){
            Toast.makeText(rootView.getContext(),
                    "Click ListItem Number " , Toast.LENGTH_LONG)
                    .show();
        }
    }
}