package org.emstrack.ambulance.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
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
 * Connects Equipment data to the RecyclerView (called from EquipmentFragment)
 * @author James Basa
 * @since 2/17/2020
 * TODO: fix these functions so that EquipmentFragment works, this file might not
 *      be needed if you choose not to use an adapter in EquipmentFragment
 */

public class EquipmentExpandableRecyclerAdapter extends RecyclerView.Adapter {

    private static final String TAG = EquipmentExpandableRecyclerAdapter.class.getSimpleName();
    private Context context;
    //List<EquipmentExpandableGroup> groups;
    List<EquipmentItem> equipment;

    public EquipmentExpandableRecyclerAdapter(Context context, List<EquipmentItem> equipment) {
        this.context = context;
        this.equipment = equipment;
    }

    @NonNull
    @Override
    //initialize ViewHolder
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.equipment_item, parent, false);
        return new EquipmentViewHolder(view, context);
    }

    @Override
    //bind each ViewHolder to the adapter
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((EquipmentViewHolder) holder).setEquipmentName(equipment.get(position).getEquipmentName());
        ((EquipmentViewHolder) holder).setValue(equipment.get(position).getValue());
        ((EquipmentViewHolder) holder).setEquipmentType(equipment.get(position).getEquipmentType());
        ((EquipmentViewHolder) holder).setComment(equipment.get(position).getComment());
    }

    @Override
    public int getItemCount() {
        return equipment.size();
    }


    /*
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
    */

}

