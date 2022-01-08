package org.emstrack.ambulance.fragments;

import android.content.BroadcastReceiver;
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
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.AmbulanceRecyclerAdapter;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.util.RequestPermission;
import org.emstrack.models.Ambulance;

public class AmbulancesFragment extends Fragment {

    private static final String TAG = AmbulanceFragment.class.getSimpleName();
    private MainActivity activity;
    private RecyclerView recyclerView;
    private AmbulancesUpdateBroadcastReceiver receiver;
    private RequestPermission requestPermission;

    public class AmbulancesUpdateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent ) {
            if (intent != null) {
                final String action = intent.getAction();
                if (action != null) {
                    if (action.equals(AmbulanceForegroundService.BroadcastActions.OTHER_AMBULANCES_UPDATE)) {

                        // TODO: update only what changed

                        Log.i(TAG, "AMBULANCES_UPDATE");
                        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                        refreshData(appData.getAmbulances());

                    }
                }
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_ambulances, container, false);
        activity = (MainActivity) requireActivity();

        recyclerView = rootView.findViewById(R.id.ambulances_recycler_view);

        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        refreshData(appData.getAmbulances());

        requestPermission = new RequestPermission(this);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        activity.setupNavigationBar();

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.OTHER_AMBULANCES_UPDATE);
        receiver = new AmbulancesUpdateBroadcastReceiver();
        getLocalBroadcastManager().registerReceiver(receiver, filter);

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // updateAmbulance UI
        refreshData(appData.getAmbulances());

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

    private void refreshData(SparseArray<Ambulance> ambulances) {

        // fast return if no ambulances
        if (ambulances == null) {
            Log.d(TAG, "No ambulances were given.");
            return;
        }

        Log.i(TAG,"Updating ambulances UI.");

        // Install adapter
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
        AmbulanceRecyclerAdapter adapter =
                new AmbulanceRecyclerAdapter(getActivity(), ambulances, this::selectAmbulance);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);

    }

    public void selectAmbulance(int ambulanceId) {
        requestPermission.setOnPermissionGranted(granted -> {
            activity.selectAmbulance(ambulanceId);
        });
        requestPermission.check();
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
