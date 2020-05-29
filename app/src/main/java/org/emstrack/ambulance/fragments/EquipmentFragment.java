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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.EquipmentExpandableRecyclerAdapter;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.EquipmentItem;

import java.util.List;

/**
 * Functionality for the Equipment Tab.
 * Edited by James on 3/8/2020.
 */
public class EquipmentFragment extends Fragment {

    private static final String TAG = EquipmentFragment.class.getSimpleName();

    View rootView;
    RecyclerView recyclerView;
    EquipmentUpdateBroadcastReceiver receiver;

    public class EquipmentUpdateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent ) {

            if (intent != null) {

                final String action = intent.getAction();

                if (action.equals(AmbulanceForegroundService.BroadcastActions.AMBULANCE_EQUIPMENTS_UPDATE)) {

                    Log.i(TAG, "AMBULANCE_EQUIPMENTS_UPDATE");
                    AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                    update(appData.getEquipments());

                }


            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_equipment, container, false);
        recyclerView = rootView.findViewById(R.id.recycler_view_equipment);

        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        update(appData.getEquipments());

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.AMBULANCE_EQUIPMENTS_UPDATE);
        receiver = new EquipmentFragment.EquipmentUpdateBroadcastReceiver();
        getLocalBroadcastManager().registerReceiver(receiver, filter);

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // updateAmbulance UI
        update(appData.getEquipments());

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
     * Update equipments list
     *
     * @param equipments list of ...
     */
    public void update(List<EquipmentItem> equipments) {



        // fast return if no equipment
        if (equipments == null) {
            Log.i(TAG, "No equipments for this ambulance.");
            return;
        }




        Log.i(TAG,"Updating equipments UI with " + equipments.size() + " items.");

        // Install fragment
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        EquipmentExpandableRecyclerAdapter adapter =
                new EquipmentExpandableRecyclerAdapter(getActivity(), equipments);
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