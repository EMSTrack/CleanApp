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
import org.emstrack.ambulance.adapters.EquipmentListRecyclerAdapter;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.util.SparseArrayUtils;
import org.emstrack.models.EquipmentItem;

import java.util.ArrayList;
import java.util.List;

public class EquipmentFragment extends Fragment {

    private static final String TAG = EquipmentFragment.class.getSimpleName();

    View rootView;
    RecyclerView recyclerView;
    EquipmentUpdateBroadcastReceiver receiver;
    List<EquipmentItem> equipmentList;
    EquipmentListRecyclerAdapter adapter;

    public class EquipmentUpdateBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                final String action = intent.getAction();
                if (action.equals(AmbulanceForegroundService.BroadcastActions.EQUIPMENT_LIST_UPDATE)) {

                    Log.i(TAG, "EQUIPMENT_LIST_UPDATE");
                    AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                    updateList(appData.getEquipmentList());

                } else if (action.equals(AmbulanceForegroundService.BroadcastActions.EQUIPMENT_ITEM_UPDATE)) {

                    Log.i(TAG, "EQUIPMENT_ITEM_UPDATE");
                    int itemId = intent.getIntExtra(AmbulanceForegroundService.BroadcastExtras.EQUIPMENT_ID, -1);
                    AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                    updateItem(appData.getEquipmentItem(itemId));
                }
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_equipment, container, false);
        recyclerView = rootView.findViewById(R.id.recycler_view);
        equipmentList = new ArrayList<EquipmentItem>();

        Log.d(TAG, "Retrieving equipment list in EquipmentFragment");
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        updateList(appData.getEquipmentList());

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AmbulanceForegroundService.BroadcastActions.EQUIPMENT_LIST_UPDATE);
        receiver = new EquipmentUpdateBroadcastReceiver();
        getLocalBroadcastManager().registerReceiver(receiver, filter);

        // get app data
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();

        // update equipment list
        updateList(appData.getEquipmentList());
    }

    @Override
    public void onPause() {
        super.onPause();

        // unregsiter receiver
        if (receiver != null) {
            getLocalBroadcastManager().unregisterReceiver(receiver);
            receiver = null;
        }
    }

    /**
     * Update equipment list
     *
     * @param equipment list of equipment
     */
    public void updateList(SparseArray<EquipmentItem> equipment) {

        if (equipment == null)
            return;

        Log.i(TAG, "Updating equipment UI");

        List<EquipmentItem> equipmentList = new ArrayList<EquipmentItem>();

        // loop over equipment list
        for (EquipmentItem item : SparseArrayUtils.iterable(equipment)) {
            // add to equipment list
            equipmentList.add(item);
        }

        this.equipmentList = equipmentList;

        // install fragment
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        adapter = new EquipmentListRecyclerAdapter(getContext(), equipmentList);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Update item in equipment list
     *
     * @param equipmentItem equipment item to update
     */
    public void updateItem(EquipmentItem equipmentItem) {

        if (equipmentItem == null)
            return;

        Log.i(TAG, "Update equipment UI");

        int itemPos = equipmentList.indexOf(equipmentItem);

        if (itemPos == -1) {
            // item not in list
            equipmentList.add(equipmentItem);
        }

        adapter.notifyItemChanged(itemPos);

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