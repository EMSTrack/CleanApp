package org.emstrack.ambulance.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.views.EquipmentViewHolder;
import org.emstrack.models.EquipmentItem;

import java.util.List;

/**
 * Connects Equipment data to the RecyclerView (called from EquipmentFragment)
 * @author James Basa
 * @since 2/17/2020
 */

public class EquipmentExpandableRecyclerAdapter extends RecyclerView.Adapter {

    private static final String TAG = EquipmentExpandableRecyclerAdapter.class.getSimpleName();
    private Context context;
    List<EquipmentItem> equipments;

    public EquipmentExpandableRecyclerAdapter(Context context, List<EquipmentItem> equipments) {
        this.context = context;
        this.equipments = equipments;
    }

    @NonNull
    @Override
    //initialize ViewHolder
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.equipment_item, parent, false);
        return new EquipmentViewHolder(context, view);
    }

    @Override
    //bind each ViewHolder to the adapter
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((EquipmentViewHolder) holder).setEquipmentName(equipments.get(position).getEquipmentName(), equipments.get(position).getEquipmentType(), equipments.get(position).getValue(), context);
        ((EquipmentViewHolder) holder).setEquipmentValue(equipments.get(position).getValue());
        ((EquipmentViewHolder) holder).setEquipmentDateUpdated(equipments.get(position).getUpdatedOn());
        ((EquipmentViewHolder) holder).setEquipmentComment(equipments.get(position).getComment());
    }

    @Override
    public int getItemCount() {
        return equipments.size();
    }

}

