package org.emstrack.ambulance.adapters;

import android.app.Activity;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.views.HospitalViewHolder;
import org.emstrack.ambulance.R;
import org.emstrack.models.Hospital;

/**
 * Connects Equipment data to the RecyclerView (called from EquipmentFragment)
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class HospitalRecyclerAdapter extends RecyclerView.Adapter<HospitalViewHolder> {

    private static final String TAG = HospitalRecyclerAdapter.class.getSimpleName();
    private final Activity activity;
    SparseArray<Hospital> hospitals;

    public HospitalRecyclerAdapter(Activity activity, SparseArray<Hospital> hospitals) {
        this.activity = activity;
        this.hospitals = hospitals;
    }

    @NonNull
    @Override
    //initialize ViewHolder
    public HospitalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.hospital_item, parent, false);
        return new HospitalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HospitalViewHolder holder, int position) {

        Hospital item = hospitals.valueAt(position);
        holder.setHospital(item, activity);

    }

    @Override
    public int getItemCount() {
        return hospitals.size();
    }

}

