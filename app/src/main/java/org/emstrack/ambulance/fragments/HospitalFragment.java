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

import org.emstrack.ambulance.dialogs.AlertSnackbar;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.HospitalExpandableRecyclerAdapter;
import org.emstrack.ambulance.models.HospitalExpandableGroup;
import org.emstrack.ambulance.services.OnServiceComplete;
import org.emstrack.models.EquipmentItem;
import org.emstrack.models.Hospital;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_hospital, container, false);
        recyclerView = rootView.findViewById(R.id.recycler_view);

        Map<Integer, Hospital> hospitals = AmbulanceForegroundService.getHospitals();

        // Retrieve hospitals?
        if (hospitals == null) {

            // Retrieve hospitals
            Intent hospitalsIntent = new Intent(getContext(), AmbulanceForegroundService.class);
            hospitalsIntent.setAction(AmbulanceForegroundService.Actions.GET_HOSPITALS);

            // What to do when GET_HOSPITALS service completes?
            new OnServiceComplete(getContext(),
                    AmbulanceForegroundService.BroadcastActions.SUCCESS,
                    AmbulanceForegroundService.BroadcastActions.FAILURE,
                    hospitalsIntent) {

                @Override
                public void onSuccess(Bundle extras) {

                    Log.i(TAG,"Got all hospitals.");

                    // updateAmbulance hospitals
                    update(AmbulanceForegroundService.getHospitals());

                }
            }
                    .setFailureMessage(getString(R.string.couldNotRetrieveHospitals))
                    .setAlert(new AlertSnackbar(getActivity()));

        } else {

            // Already have hospitals
            update(hospitals);

        }

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

        // updateAmbulance UI
        update(AmbulanceForegroundService.getHospitals());

    }

    @Override
    public void onPause() {
        super.onPause();

        // Unregister receiver
        if (receiver != null) {
            getLocalBroadcastManager().unregisterReceiver(receiver);
            receiver = null;
        }

    }

    /**
     * Update hospital list
     *
     * @param hospitals list of hospitals
     */
    public void update(Map<Integer, Hospital> hospitals) {

        // fast return if no hospitals
        if (hospitals == null)
            return;

        Log.i(TAG,"Updating hospitals UI.");

        // Create HospitalExpandableGroup
        final List hospitalExpandableGroup = new ArrayList<HospitalExpandableGroup>();

        // Retrieve hospitals
        ArrayList<Hospital> sortedHospitals = new ArrayList<>();
        for (Map.Entry<Integer, Hospital> entry : hospitals.entrySet())
            sortedHospitals.add( entry.getValue() );

        // Sort hospitals
        Collections.sort(sortedHospitals, new Hospital.SortByName());

        // Loop over all hospitals
        for (Hospital hospital : sortedHospitals ) {

            // Add to to expandable group
            hospitalExpandableGroup.add(
                    new HospitalExpandableGroup(hospital.getName(),
                            new ArrayList<EquipmentItem>(), // hospital.getHospitalequipmentSet(),
                            hospital));

        }

        /*
        // Loop over all hospitals
        for (Map.Entry<Integer, Hospital> entry : hospitals.entrySet()) {

            // Get hospital
            Hospital hospital = entry.getValue();

            // Add to to expandable group
            hospitalExpandableGroup.add(
                    new HospitalExpandableGroup(hospital.getName(),
                            new ArrayList<EquipmentItem>(), // hospital.getHospitalequipmentSet(),
                            hospital));

        }
        */

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