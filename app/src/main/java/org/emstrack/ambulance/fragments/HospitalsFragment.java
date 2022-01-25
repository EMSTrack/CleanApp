package org.emstrack.ambulance.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.HospitalRecyclerAdapter;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.util.FragmentWithLocalBroadcastReceiver;
import org.emstrack.models.Hospital;

public class HospitalsFragment extends FragmentWithLocalBroadcastReceiver {

    private static final String TAG = HospitalsFragment.class.getSimpleName();
    private RecyclerView recyclerView;

    @Override
    public void onReceive(Context context, @NonNull Intent intent ) {
        final String action = intent.getAction();
        if (action != null) {
            if (action.equals(AmbulanceForegroundService.BroadcastActions.HOSPITALS_UPDATE)) {

                Log.i(TAG, "HOSPITALS_UPDATE");
                AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                update(appData.getHospitals());

            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_hospital, container, false);

        recyclerView = rootView.findViewById(R.id.hospital_recycler_view);

        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        update(appData.getHospitals());

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.HOSPITALS_UPDATE);
        setupReceiver(filter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity activity = (MainActivity) requireActivity();
        activity.setupNavigationBar();

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // updateAmbulance UI
        update(appData.getHospitals());

    }

    /**
     * Update hospital list
     *
     * @param hospitals list of hospitals
     */
    public void update(SparseArray<Hospital> hospitals) {

        // fast return if no hospitals
        if (hospitals == null)
            return;

        Log.i(TAG,"Updating hospitals UI.");

        try {
            // Install adapter
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
            HospitalRecyclerAdapter adapter =
                    new HospitalRecyclerAdapter(requireActivity(), hospitals);
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.setAdapter(adapter);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Illegal activity");
            e.printStackTrace();
        }

    }

}