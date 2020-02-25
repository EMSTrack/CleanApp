package org.emstrack.ambulance.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thoughtbot.expandablerecyclerview.ExpandableRecyclerViewAdapter;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.models.EquipmentExpandableGroup;
import org.emstrack.ambulance.models.HospitalExpandableGroup;
import org.emstrack.ambulance.views.EquipmentViewHolder;
import org.emstrack.ambulance.views.HospitalViewHolder;
import org.emstrack.models.EquipmentItem;
import org.emstrack.models.Hospital;

import java.util.List;

/**
 * Created by James on 2/17/2020. TODO: fix overridden functions
 */

public class EquipmentExpandableRecyclerAdapter
        extends ExpandableRecyclerViewAdapter<EquipmentViewHolder, EquipmentViewHolder> {

    private static final String TAG = EquipmentExpandableRecyclerAdapter.class.getSimpleName();

    public EquipmentExpandableRecyclerAdapter(List<? extends ExpandableGroup> groups) {
        super(groups);
    }

    @Override
    public EquipmentViewHolder onCreateGroupViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.equipment_item, parent, false);
        return new EquipmentViewHolder(view);
    }


    @Override
    public EquipmentViewHolder onCreateChildViewHolder(ViewGroup child, int viewType) {
        //View view = LayoutInflater.from(child.getContext()).inflate(R.layout.equipment_item_details, child, false);
        //return new EquipmentViewHolder(view);
    }


    @Override
    public void onBindChildViewHolder(EquipmentViewHolder holder, int flatPosition,
                                      ExpandableGroup group, int childIndex) {
        EquipmentItem hospitalEquipment = ((HospitalExpandableGroup) group).getItems().get(childIndex);
        // Log.d(TAG, "Binding equipment '" + hospitalEquipment + "'");

        //holder.setHospitalEquipment(hospitalEquipment);
    }



    @Override
    public void onBindGroupViewHolder(EquipmentViewHolder holder, int flatPosition,
                                      ExpandableGroup group) {
        //EquipmentItem equipment = ((EquipmentExpandableGroup) group).getEquipment();
        // Log.d(TAG, "Binding hospital '" + hospital.getName() + "'");

        //holder.setEquipment(equipment);
    }


}

