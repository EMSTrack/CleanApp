package org.emstrack.ambulance.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.views.HospitalEquipmentViewHolder;
import org.emstrack.models.EquipmentItem;

public class EquipmentListRecyclerAdapter extends RecyclerView.Adapter<HospitalEquipmentViewHolder> {

    private static final String TAG = EquipmentListRecyclerAdapter.class.getSimpleName();

    private SparseArray<EquipmentItem> equipment;
    private LayoutInflater inflater;

    public EquipmentListRecyclerAdapter(Context context, SparseArray<EquipmentItem> equipment) {
        this.equipment = equipment;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public HospitalEquipmentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.hospital_equipment_item, parent, false);
        return new HospitalEquipmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(HospitalEquipmentViewHolder holder, int position) {
        EquipmentItem item = equipment.valueAt(position);
        holder.setHospitalEquipment(item);
    }

    @Override
    public int getItemCount() {
        return equipment.size();
    }
}
