package org.emstrack.ambulance.adapters;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.views.HospitalRecyclerViewViewHolder;
import org.emstrack.models.Hospital;

/**
 * Connects Equipment data to the RecyclerView (called from EquipmentFragment)
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class HospitalRecyclerAdapter extends RecyclerView.Adapter<HospitalRecyclerViewViewHolder> {

    private static final String TAG = HospitalRecyclerAdapter.class.getSimpleName();
    private final Context context;
    SparseArray<Hospital> hospitals;

    public HospitalRecyclerAdapter(Context context, SparseArray<Hospital> hospitals) {
        this.context = context;
        this.hospitals = hospitals;
    }

    @NonNull
    @Override
    //initialize ViewHolder
    public HospitalRecyclerViewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.hospital_item, parent, false);
        return new HospitalRecyclerViewViewHolder(context, view);
    }

    @Override
    public void onBindViewHolder(@NonNull HospitalRecyclerViewViewHolder holder, int position) {

        Hospital item = hospitals.valueAt(position);
        holder.setHospital(item, context);

    }

    @Override
    public int getItemCount() {
        return hospitals.size();
    }

}

