package org.emstrack.ambulance.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Toast;

import org.emstrack.ambulance.R;
import org.emstrack.models.HospitalPermission;

import java.util.ArrayList;

/**
 * This class is purely meant to demonstrate that the information is able to send
 * to the server. The website is:
 *
 * http://cruzroja.ucsd.edu/ambulances/info/123456
 *
 *
 */
public class HospitalActivity extends Fragment {

    View rootView;
    ListView listView;

    private ArrayList<HospitalPermission> hospitalList;
    private ExpandableListView hospitalExpandableList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.activity_hospital, container, false);

/*
        hospitalList = Hospital.getHospitals();

        if (hospitalList == null) {
            return view;
        }

        HospitalAdapter adapter = new HospitalAdapter(view.getContext(), hospitalList);
        hospitalExpandableList = (ExpandableListView) view.findViewById(R.id.equipment_listview);
        hospitalExpandableList.setAdapter(adapter);
*/

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