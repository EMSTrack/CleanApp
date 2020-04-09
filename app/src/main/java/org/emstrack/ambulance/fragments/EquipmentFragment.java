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
import org.emstrack.models.Hospital;

import java.util.ArrayList;
import java.util.List;

/** Edited by James on 3/8/2020.
 *  TODO: Uncomment the commented code and update it so that it works for
 *          equipment instead of hospitals
 */
public class EquipmentFragment extends Fragment {

    private static final String TAG = EquipmentFragment.class.getSimpleName();

    View rootView;
    RecyclerView recyclerView;

    //TODO: set up EQUIPMENT_UPDATE and getEquipment
    EquipmentUpdateBroadcastReceiver receiver;

    public class EquipmentUpdateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent ) {

            if (intent != null) {
                //this is left over from HospitalFragment, it's not needed

                final String action = intent.getAction();
                // TODO: set up EQUIPMENT_UPDATE and getEquipment() so we can broadcast the equipment
                if (action.equals(AmbulanceForegroundService.BroadcastActions.EQUIPMENT_UPDATE)) {

                    Log.i(TAG, "EQUIPMENT_UPDATE");
                    AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                    update(appData.getEquipment());


                }


            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_equipment, container, false);
        recyclerView = rootView.findViewById(R.id.recycler_view);

        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        update(appData.getEquipment());

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.EQUIPMENT_UPDATE);
        receiver = new EquipmentFragment.EquipmentUpdateBroadcastReceiver();
        getLocalBroadcastManager().registerReceiver(receiver, filter);

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // updateAmbulance UI
        update(appData.getEquipment());

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

    //TODO function: change Hospital to EquipmentItem for all of this code, might not need equipmentExpandableGroup
    /**
     * Update equipment list
     *
     * @param equipment list of ...
     */
    public void update(List<EquipmentItem> equipment) {

        // fast return if no equipment
        if (equipment == null)
            return;

        Log.i(TAG,"Updating equipment UI.");

        /*
        // Create EquipmentExpandableGroups
        final List equipmentExpandableGroups = new ArrayList<EquipmentExpandableGroup>();

        // Loop over all equipment
        for (EquipmentItem item : SparseArrayUtils.iterable(equipment) ) {

            // Add to expandable group list
            //TODO: retrieve dynamic data
            equipmentExpandableGroups.add(
                    new EquipmentExpandableGroup(item,
                            "",
                            "",
                            ""));

        }*/
        // Install fragment
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        EquipmentExpandableRecyclerAdapter adapter =
                new EquipmentExpandableRecyclerAdapter(equipment, getActivity());
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