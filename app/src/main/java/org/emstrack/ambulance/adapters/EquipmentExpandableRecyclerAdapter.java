package org.emstrack.ambulance.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thoughtbot.expandablerecyclerview.ExpandableRecyclerViewAdapter;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.models.EquipmentExpandableGroup;
import org.emstrack.ambulance.models.HospitalExpandableGroup;
import org.emstrack.ambulance.views.EquipmentViewHolder;
import org.emstrack.ambulance.views.HospitalEquipmentViewHolder;
import org.emstrack.ambulance.views.HospitalViewHolder;
import org.emstrack.models.EquipmentItem;
import org.emstrack.models.Hospital;

import java.util.List;

/**
 * Created by James on 2/17/2020. This file is called from EquipmentFragment.
 * It allows for the app to know what to display for Equipment (there is a parent item,
 * which is the main item shown on each Card, and there is a child item, which is
 * shown when the parent is clicked on and the Card is expanded)
 * TODO: fix these functions so that EquipmentFragment works, this file might not
 *      be needed if you choose not to use an adapter in EquipmentFragment
 */

public class EquipmentExpandableRecyclerAdapter
        extends ExpandableRecyclerViewAdapter<EquipmentViewHolder, HospitalEquipmentViewHolder> {

    private static final String TAG = EquipmentExpandableRecyclerAdapter.class.getSimpleName();
    private Context context;

    public EquipmentExpandableRecyclerAdapter(List<? extends ExpandableGroup> groups, Context context) {
        super(groups);
        this.context = context;
    }

    @Override
    public EquipmentViewHolder onCreateGroupViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.equipment_item, parent, false);
        return new EquipmentViewHolder(view, context);
    }


    @Override // TODO: change hospital_equipment_item
    public HospitalEquipmentViewHolder onCreateChildViewHolder(ViewGroup child, int viewType) {
        View view = LayoutInflater.from(child.getContext()).inflate(R.layout.hospital_equipment_item, child, false);
        return new HospitalEquipmentViewHolder(view);
    }


    @Override
    public void onBindChildViewHolder(HospitalEquipmentViewHolder holder, int flatPosition,
                                      ExpandableGroup group, int childIndex) {
        EquipmentItem hospitalEquipment = ((HospitalExpandableGroup) group).getItems().get(childIndex);
        // Log.d(TAG, "Binding equipment '" + hospitalEquipment + "'");

        holder.setHospitalEquipment(hospitalEquipment);
    }



    @Override //TODO: change hospital line to equipment
    public void onBindGroupViewHolder(EquipmentViewHolder holder, int flatPosition,
                                      ExpandableGroup group) {
        //EquipmentItem equipment = ((EquipmentExpandableGroup) group).getEquipment();
        // Log.d(TAG, "Binding hospital '" + hospital.getName() + "'");

        //holder.setEquipment(equipment);
    }


}

