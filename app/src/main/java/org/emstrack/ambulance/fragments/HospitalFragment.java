package org.emstrack.ambulance.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Toast;

import org.emstrack.ambulance.AmbulanceApp;
import org.emstrack.ambulance.HospitalAdapter;
import org.emstrack.models.Hospital;
import org.emstrack.mqtt.MqttProfileClient;
import org.emstrack.ambulance.R;
import org.emstrack.models.Hospital;

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

    View rootView;
    ListView listView;

    private ExpandableListView hospitalExpandableList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_hospital, container, false);
        final MqttProfileClient profileClient = ((AmbulanceApp) getActivity().getApplication()).getProfileClient();
        final List<Hospital> hospitals = profileClient.getProfile().getHospitals();

        if (hospitals == null) {
            return rootView;
        }

        HospitalAdapter adapter = new HospitalAdapter(rootView.getContext(), hospitals);
        hospitalExpandableList = (ExpandableListView) rootView.findViewById(R.id.equipment_listview);
        hospitalExpandableList.setAdapter(adapter);


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