package org.emstrack.ambulance.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.emstrack.ambulance.R;
//TODO change hospital imports to equipment imports below
import org.emstrack.ambulance.adapters.EquipmentRecyclerAdapter;
import org.emstrack.ambulance.dialogs.AlertDialog;
import org.emstrack.ambulance.dialogs.AlertSnackbar;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.util.SwipeController;
import org.emstrack.ambulance.util.SwipeControllerActions;
import org.emstrack.models.Ambulance;
import org.emstrack.models.EquipmentItem;
import org.emstrack.models.api.APIService;
import org.emstrack.models.api.APIServiceGenerator;
import org.emstrack.models.api.OnAPICallComplete;
import org.emstrack.models.util.BroadcastActions;
import org.emstrack.models.util.OnServiceComplete;

import java.util.List;

/**
 * Functionality for the Equipment Tab.
 * Edited by James on 3/8/2020.
 */
public class EquipmentFragment extends Fragment {

    private static final String TAG = EquipmentFragment.class.getSimpleName();

    private View rootView;
    private RecyclerView recyclerView;
    private TextView refreshingData;
    private SwipeController swipeController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_equipment, container, false);
        refreshingData = rootView.findViewById(R.id.equipment_refreshing_data);

        swipeController = new SwipeController(getContext(), new SwipeControllerActions(){
            @Override
            public void onLeftClicked(int position) {
                new AlertDialog(getActivity(), getString(R.string.editEquipment))
                        .alert(getString(R.string.notImplementedYet));
            }
        },
                ItemTouchHelper.RIGHT);

        recyclerView = rootView.findViewById(R.id.equipment_recycler_view);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
                swipeController.onDraw(c);
            }
        });

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeController);
        itemTouchhelper.attachToRecyclerView(recyclerView);


        // Refresh data
        refreshData();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh data
        refreshData();

    }

    @Override
    public void onPause() {
        super.onPause();
    }


    /**
     * refreshData
     */
    public void refreshData() {

        // retrieve hospital equipment
        Ambulance ambulance = AmbulanceForegroundService.getAppData().getAmbulance();
        APIService service = APIServiceGenerator.createService(APIService.class);
        retrofit2.Call<List<EquipmentItem>> callAmbulanceEquipment = service.getAmbulanceEquipment(ambulance.getId());

        refreshingData.setText(R.string.refreshingData);
        refreshingData.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        new OnAPICallComplete<List<EquipmentItem>>(callAmbulanceEquipment) {

            @Override
            public void onSuccess(List<EquipmentItem> equipments) {

                // hide refresh label
                refreshingData.setVisibility(View.GONE);

                // Install adapter
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
                EquipmentRecyclerAdapter adapter =
                        new EquipmentRecyclerAdapter(getContext(), equipments);
                recyclerView.setLayoutManager(linearLayoutManager);
                recyclerView.setAdapter(adapter);

                recyclerView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Throwable t) {
                super.onFailure(t);

                refreshingData.setText(R.string.couldNotRetrieveEquipments);

            }
        }
                .start();

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