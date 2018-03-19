package org.emstrack.ambulance.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.emstrack.ambulance.AmbulanceForegroundService;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.HospitalExpandableRecyclerAdapter;
import org.emstrack.ambulance.models.HospitalExpandableGroup;
import org.emstrack.models.Ambulance;
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

    private static final String TAG = HospitalFragment.class.getSimpleName();

    View rootView;
    RecyclerView recyclerView;
    HospitalsUpdateBroadcastReceiver receiver;

    public class HospitalsUpdateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent ) {
            if (intent != null) {
                final String action = intent.getAction();
                if (action.equals(AmbulanceForegroundService.BroadcastActions.HOSPITALS_UPDATE)) {

                    Log.i(TAG, "HOSPITALS_UPDATE");
                    update(AmbulanceForegroundService.getHospitals());

                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_hospital, container, false);
        recyclerView = rootView.findViewById(R.id.recycler_view);

        List<Hospital> hospitals = AmbulanceForegroundService.getHospitals();
        if (hospitals != null)
            update(hospitals);

        return rootView;

    }

    @Override
    public void onResume() {
        super.onResume();

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.HOSPITALS_UPDATE);
        receiver = new HospitalsUpdateBroadcastReceiver();
        getLocalBroadcastManager().registerReceiver(receiver, filter);

    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister receiver
        if (receiver != null) {
            getLocalBroadcastManager().unregisterReceiver(receiver);
            receiver = null;
        }
        super.onDestroy();
    }

    /**
     * Update hospital list
     *
     * @param hospitals list of hospitals
     */
    public void update(List<Hospital> hospitals) {

        // Loop through hospitals
        final List hospitalExpandableGroup = new ArrayList<HospitalExpandableGroup>();
        for (Hospital hospital : hospitals)

            // Add to to expandable group
            hospitalExpandableGroup.add(
                    new HospitalExpandableGroup(hospital.getName(),
                            hospital.getHospitalequipmentSet(),
                            hospital));

        // Install fragment
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        HospitalExpandableRecyclerAdapter adapter =
                new HospitalExpandableRecyclerAdapter(hospitalExpandableGroup);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);

    }

    /**
     * Get LocalBroadcastManager
     *
     * @return the LocalBroadcastManager
     */
    private LocalBroadcastManager getLocalBroadcastManager() {
        return LocalBroadcastManager.getInstance(getContext());
    }

}