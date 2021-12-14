package org.emstrack.ambulance.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.views.EquipmentRecyclerViewViewHolder;
import org.emstrack.models.EquipmentItem;

import java.util.List;

/**
 * Connects Equipment data to the RecyclerView (called from EquipmentFragment)
 * @author James Basa
 * @since 2/17/2020
 */

public class EquipmentRecyclerAdapter extends RecyclerView.Adapter<EquipmentRecyclerViewViewHolder> {

    private static final String TAG = EquipmentRecyclerAdapter.class.getSimpleName();
    private final Context context;
    List<EquipmentItem> equipments;

    public EquipmentRecyclerAdapter(Context context, List<EquipmentItem> equipments) {
        this.context = context;
        this.equipments = equipments;
    }

    @NonNull
    @Override
    public EquipmentRecyclerViewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.equipment_item, parent, false);
        return new EquipmentRecyclerViewViewHolder(context, view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipmentRecyclerViewViewHolder holder, int position) {

        EquipmentItem item = equipments.get(position);
        holder.setEquipment(item, context);

    }

    @Override
    public int getItemCount() {
        return equipments.size();
    }

}

