package org.emstrack.ambulance.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.HospitalRecyclerAdapter;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.Hospital;

public class HospitalsFragment extends Fragment {

    private static final String TAG = HospitalsFragment.class.getSimpleName();
    private MainActivity activity;
    private RecyclerView recyclerView;
    private HospitalsUpdateBroadcastReceiver receiver;

    public class HospitalsUpdateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent ) {
            if (intent != null) {
                final String action = intent.getAction();
                if (action != null) {
                    if (action.equals(AmbulanceForegroundService.BroadcastActions.HOSPITALS_UPDATE)) {

                        Log.i(TAG, "HOSPITALS_UPDATE");
                        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                        update(appData.getHospitals());

                    }
                }
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_hospital, container, false);
        activity = (MainActivity) requireActivity();

        recyclerView = rootView.findViewById(R.id.hospital_recycler_view);

        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        update(appData.getHospitals());

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        activity.setupNavigationBar();

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // updateAmbulance UI
        update(appData.getHospitals());

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

        // Install adapter
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
        HospitalRecyclerAdapter adapter =
                new HospitalRecyclerAdapter(getActivity(), hospitals);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);

    }

    /**
     * Get LocalBroadcastManager
     *
     * @return the LocalBroadcastManager
     */
    private LocalBroadcastManager getLocalBroadcastManager() {
        return LocalBroadcastManager.getInstance(requireContext());
    }

}