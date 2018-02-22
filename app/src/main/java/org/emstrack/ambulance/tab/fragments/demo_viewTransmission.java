package org.emstrack.ambulance.tab.fragments;


import android.app.ListActivity;
import android.content.Context;
import android.support.annotation.IntegerRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.emstrack.models.AmbulancePermission;
import org.emstrack.ambulance.AmbulanceApp;
import org.emstrack.models.HospitalPermission;
//import hansyuan.cruzrojamobile.HospitalAdapter;
import org.emstrack.ambulance.R;
import org.emstrack.models.Profile;
import org.emstrack.mqtt.MqttProfileClient;

/**
 * This class is purely meant to demonstrate that the information is able to send
 * to the server. The website is:
 *
 * http://cruzroja.ucsd.edu/ambulances/info/123456
 *
 *
 */
public class demo_viewTransmission extends Fragment {
    View rootView;
    ListView listView;

    private List<HospitalPermission> hospitalList;
    private ExpandableListView hospitalExpandableList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.activity_demo_view_transmission, container, false);

        final MqttProfileClient profileClient = ((AmbulanceApp) getActivity().getApplication()).getProfileClient();
        final List<HospitalPermission> hospitalList = profileClient.getProfile().getHospitals();

        if (hospitalList == null) {
            return rootView;
        }

        //HospitalAdapter adapter = new HospitalAdapter(rootView.getContext(), hospitalList);
        //hospitalExpandableList = (ExpandableListView) rootView.findViewById(R.id.equipment_listview);
        //hospitalExpandableList.setAdapter(adapter);

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