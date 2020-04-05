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
 *          equipment instead of hospitals (they were copied over from the
 *          HospitalFragment file so they don't work, due to not having
 *          the corresponding files set up correctly yet)
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
            /* TODO: create an EQUIPMENT_UPDATE so we can broadcast the equipment
             *         info like how HOSPITALS_UPDATE is set up here
             */
            /*
            if (intent != null) {
                //this is left over from HospitalFragment, it's not needed

                final String action = intent.getAction();

                if (action.equals(AmbulanceForegroundService.BroadcastActions.EQUIPMENT_UPDATE)) {

                    Log.i(TAG, "EQUIPMENT_UPDATE");
                    AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                    update(appData.getHospitals());


                }


            }*/
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_equipment, container, false);
        recyclerView = rootView.findViewById(R.id.recycler_view);

        //TODO set up getEquipment()
        //AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        //update(appData.getHospitals());

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        //TODO set up EquipmentUpdateBroadcastReceiver() and getEquipment()
        /*
        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.HOSPITALS_UPDATE);
        receiver = new EquipmentFragment.EquipmentUpdateBroadcastReceiver();
        getLocalBroadcastManager().registerReceiver(receiver, filter);

        // Get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // updateAmbulance UI
        update(appData.getHospitals());
        */
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
     * @param hospital list of ...
     */
    public void update(SparseArray<Hospital> hospitals) {

        // fast return if no hospitals
        if (hospitals == null)
            return;

        Log.i(TAG,"Updating equipment UI.");
        /*
        // Create EquipmentExpandableGroup
        final List equipmentExpandableGroup = new ArrayList<EquipmentExpandableGroup>();

        // Loop over all equipment
        for (Hospital hospital : SparseArrayUtils.iterable(hospitals) ) {

            // Add to to expandable group
            //TODO: not sure if we should create an EquipmentExpandableGroup for each equipment
            equipmentExpandableGroup.add(
                    new EquipmentExpandableGroup(hospital.getName(),
                            new ArrayList<>(),
                            hospital));

        }
        //TODO: I changed this from HospitalExpandableRecyclerAdapter to
        //  EquipmentExpandableRecyclerAdapter, since I created that file already (check it out)
        // Install fragment
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        EquipmentExpandableRecyclerAdapter adapter =
                new EquipmentExpandableRecyclerAdapter(equipmentExpandableGroup, getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);

         */

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