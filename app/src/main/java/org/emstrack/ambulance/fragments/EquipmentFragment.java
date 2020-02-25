package org.emstrack.ambulance.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.emstrack.ambulance.R;
//TODO change hospital imports to equipment imports below
import org.emstrack.ambulance.adapters.EquipmentExpandableRecyclerAdapter;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.models.EquipmentExpandableGroup;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.util.SparseArrayUtils;
import org.emstrack.models.EquipmentItem;

import java.util.ArrayList;
import java.util.List;

public class EquipmentFragment extends Fragment {

    private static final String TAG = EquipmentFragment.class.getSimpleName();

    View rootView;
    RecyclerView recyclerView;

    //TODO set up EQUIPMENT_UPDATE and getEquipment
    EquipmentUpdateBroadcastReceiver receiver;

    public class EquipmentUpdateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent ) {
            if (intent != null) {
                final String action = intent.getAction();
                /*
                if (action.equals(AmbulanceForegroundService.BroadcastActions.EQUIPMENT_UPDATE)) {

                    Log.i(TAG, "EQUIPMENT_UPDATE");
                    AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                    update(appData.getHospitals());


                }

                 */
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_equipment, container, false);
        recyclerView = rootView.findViewById(R.id.recycler_view);

        //TODO set up getEquipment
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        //update(appData.getEquipment());

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        //TODO set up EquipmentUpdateBroadcastReceiver and getEquipment
        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.HOSPITALS_UPDATE);
        receiver = new EquipmentFragment.EquipmentUpdateBroadcastReceiver();
        getLocalBroadcastManager().registerReceiver(receiver, filter);

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // updateAmbulance UI
        //update(appData.getEquipment());
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

    //TODO function: update(), create EquipmentExpandableGroup
    /**
     * Update equipment list
     *
     * @param equipment list of equipment
     */
    public void update(SparseArray<EquipmentItem> equipment) {

        // fast return if no hospitals
        if (equipment == null)
            return;

        Log.i(TAG,"Updating equipment UI.");

        // Create EquipmentExpandableGroup
        final List equipmentExpandableGroup = new ArrayList<EquipmentExpandableGroup>();

        // Loop over all equipment
        for (EquipmentItem equipmentItem : SparseArrayUtils.iterable(equipment) ) {

            // Add to to expandable group
            //TODO not sure if we should create an EquipmentExpandableGroup for each equipment
            equipmentExpandableGroup.add(
                    new EquipmentExpandableGroup(new ArrayList<>()));

        }

        // Install fragment
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        EquipmentExpandableRecyclerAdapter adapter =
                new EquipmentExpandableRecyclerAdapter(equipmentExpandableGroup);
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