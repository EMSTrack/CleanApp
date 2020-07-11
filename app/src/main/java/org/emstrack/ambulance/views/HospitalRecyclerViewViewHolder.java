package org.emstrack.ambulance.views;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.R;
import org.emstrack.models.Hospital;

/**
 * Holds the hospital data
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class HospitalRecyclerViewViewHolder extends RecyclerView.ViewHolder {

    private final TextView hospitalName;

    private static final String TAG = HospitalRecyclerViewViewHolder.class.getSimpleName();


    public HospitalRecyclerViewViewHolder(Context context, View view) {
        super(view);

        hospitalName = view.findViewById(R.id.hospital_name);

        view.setOnClickListener(v -> {
        });

    }

    public void setHospital(Hospital item, Context context) {

        hospitalName.setText(item.getName());

    }

}