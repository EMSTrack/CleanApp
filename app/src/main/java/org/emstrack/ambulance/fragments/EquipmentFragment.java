package org.emstrack.ambulance.fragments;

import android.graphics.Canvas;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.EquipmentRecyclerAdapter;
import org.emstrack.ambulance.dialogs.SimpleAlertDialog;
import org.emstrack.ambulance.models.EquipmentType;
import org.emstrack.ambulance.util.SwipeController;
import org.emstrack.ambulance.util.SwipeControllerActions;
import org.emstrack.models.EquipmentItem;
import org.emstrack.models.api.APIService;
import org.emstrack.models.api.APIServiceGenerator;
import org.emstrack.models.api.OnAPICallComplete;

import java.util.List;

/**
 * Functionality for the Equipment Tab.
 * Edited by James on 3/8/2020.
 */
public class EquipmentFragment extends Fragment {

    private static final String TAG = EquipmentFragment.class.getSimpleName();

    private RecyclerView recyclerView;
    private TextView refreshingData;
    private SwipeController swipeController;
    private MainActivity activity;

    private EquipmentType type;
    private int id;
    private TextView equipmentType;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_equipment, container, false);
        activity = (MainActivity) requireActivity();

        refreshingData = rootView.findViewById(R.id.equipment_refreshing_data);
        equipmentType = rootView.findViewById(R.id.equipment_type);

        swipeController = new SwipeController(requireContext(), new SwipeControllerActions(){
            @Override
            public void onLeftClicked(int position) {
                new SimpleAlertDialog(getActivity(), getString(R.string.editEquipment))
                        .alert(getString(R.string.notImplementedYet));
            }
        },
                ItemTouchHelper.RIGHT);

        recyclerView = rootView.findViewById(R.id.equipment_recycler_view);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                swipeController.onDraw(c);
            }
        });

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeController);
        itemTouchhelper.attachToRecyclerView(recyclerView);

        // get arguments
        Bundle arguments = getArguments();
        if (arguments != null) {
            type = (EquipmentType) arguments.getSerializable("type");
            id = getArguments().getInt("id", -1);
        } else {
            type = EquipmentType.AMBULANCE;
            id = -1;
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // setup navigation
        activity.setupNavigationBar();

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

        MainActivity mainActivity = (MainActivity) requireActivity();

        // retrieve equipment
        refreshingData.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        if (id == -1) {

            refreshingData.setText(R.string.equipmentNotAvailable);

        } else {

            refreshingData.setText(R.string.refreshingData);

            APIService service = APIServiceGenerator.createService(APIService.class);
            retrofit2.Call<List<EquipmentItem>> callEquipment;
            String label;
            if (type == EquipmentType.AMBULANCE) {
                callEquipment = service.getAmbulanceEquipment(id);
                label = mainActivity.getAmbulanceIdentifier(id);
            } else { // if (type == EquipmentType.HOSPITAL)
                callEquipment = service.getHospitalEquipment(id);
                label = mainActivity.getHospitalName(id);
            }
            equipmentType.setText(label);
            equipmentType.setVisibility(View.VISIBLE);

            refreshingData.setText(R.string.refreshingData);

            new OnAPICallComplete<List<EquipmentItem>>(callEquipment) {

                @Override
                public void onSuccess(List<EquipmentItem> equipments) {

                    if (equipments.size() > 0) {

                        // hide refresh label
                        refreshingData.setVisibility(View.GONE);

                        // Install adapter
                        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
                        EquipmentRecyclerAdapter adapter =
                                new EquipmentRecyclerAdapter(getContext(), equipments);
                        recyclerView.setLayoutManager(linearLayoutManager);
                        recyclerView.setAdapter(adapter);

                        recyclerView.setVisibility(View.VISIBLE);

                    } else {

                        refreshingData.setText(getString(R.string.noEquipmentText, label));

                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    super.onFailure(t);

                    refreshingData.setText(R.string.couldNotRetrieveEquipments);

                }
            }
                    .start();
        }
    }

}