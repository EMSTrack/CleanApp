package org.emstrack.ambulance;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.emstrack.models.Hospital;
import org.emstrack.models.HospitalEquipment;

import java.util.List;

/**
 * Created by tina on 3/6/18.
 */

public class HospitalRVAdapter extends RecyclerView.Adapter<HospitalRVAdapter.HospitalViewHolder> {
    private static String TAG = HospitalRVAdapter.class.getSimpleName();
    private Context context;
    private List<Hospital> hospitals;


    public HospitalRVAdapter(Context context, List<Hospital> hospitals) {
        this.context = context;
        this.hospitals = hospitals;
    }

    @Override
    public HospitalViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.rv_hospital_item, parent, false);
        return new HospitalViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(HospitalViewHolder holder, int position) {
        Hospital hospital = hospitals.get(position);
        holder.setHospitalName(hospital.getHospitalName());
        holder.setClickListener(hospital.getHospitalId(), hospital.getHospitalEquipment());
    }

    @Override
    public int getItemCount() {
        return hospitals.size();
    }

    static class HospitalViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView hospitalImageIV;
        TextView hospitalNameTV;

        private HospitalViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cv);
            hospitalImageIV = itemView.findViewById(R.id.hospital_image);
            hospitalNameTV = itemView.findViewById(R.id.hospital_name);
        }

        private void setHospitalName(String hospitalName) {
            hospitalNameTV.setText(hospitalName);
        }

        private void setHospitalImageIV() {
        }

        private void setClickListener(final int hospitalId, List<HospitalEquipment> hospitalEquipment) {
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.e(TAG, "Clicked Hospital: " + hospitalNameTV.getText().toString());
                }
            });
        }
    }
}
