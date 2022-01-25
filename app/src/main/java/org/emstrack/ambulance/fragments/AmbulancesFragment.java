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
import org.emstrack.ambulance.adapters.AmbulanceRecyclerAdapter;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.util.FragmentWithLocalBroadcastReceiver;
import org.emstrack.ambulance.util.RequestPermission;
import org.emstrack.models.Ambulance;
import org.emstrack.models.UpdatedOn;

import java.util.ArrayList;
import java.util.Collections;

public class AmbulancesFragment extends FragmentWithLocalBroadcastReceiver {

    private static final String TAG = AmbulanceFragment.class.getSimpleName();
    private MainActivity activity;
    private RecyclerView recyclerView;
    private RequestPermission requestPermission;

    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_ambulances, container, false);
        activity = (MainActivity) requireActivity();

        recyclerView = rootView.findViewById(R.id.ambulances_recycler_view);

        requestPermission = new RequestPermission(this);

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.OTHER_AMBULANCES_UPDATE);
        setupReceiver(filter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        activity.setupNavigationBar();

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // updateAmbulance UI
        refreshData(appData.getAmbulances());

    }

    private void refreshData(SparseArray<Ambulance> ambulances) {

        // fast return if no ambulances
        if (ambulances == null) {
            Log.d(TAG, "No ambulances were given.");
            return;
        }

        Log.i(TAG,"Updating ambulances UI.");

        // convert sparse array to list and sort by updatedOn
        ArrayList<Ambulance> ambulanceList = new ArrayList<>();
        for (int i = 0; i < ambulances.size(); i++) {
            ambulanceList.add(ambulances.valueAt(i));
        }
        Collections.sort(ambulanceList, new UpdatedOn.SortDescending());

        try {
            // Install adapter
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
            AmbulanceRecyclerAdapter adapter =
                    new AmbulanceRecyclerAdapter(requireActivity(), ambulanceList, this::selectAmbulance);
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.setAdapter(adapter);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Invalid context or activity");
            e.printStackTrace();
        }

    }

    public void selectAmbulance(int ambulanceId) {
        requestPermission.setOnPermissionGranted(granted -> activity.selectAmbulance(ambulanceId));
        requestPermission.check();
    }

}
