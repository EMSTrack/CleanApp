package org.emstrack.ambulance.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.emstrack.ambulance.R;
import org.emstrack.models.HospitalEquipment;

import java.util.ArrayList;

/**
 * Created by tina on 3/6/18.
 */

public class HospitalEquipmentRVAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static String TAG = HospitalEquipmentRVAdapter.class.getSimpleName();
    private static int HEADER_VIEW_TYPE = 0;
    private static int EQUIPMENT_VIEW_TYPE = 1;
    private ArrayList<HospitalEquipment> hospitalEquipmentList;
    private String hospitalName;

    public HospitalEquipmentRVAdapter(String hospitalName, ArrayList<HospitalEquipment> hospitalEquipment) {
        this.hospitalName = hospitalName;
        this.hospitalEquipmentList = hospitalEquipment;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == HEADER_VIEW_TYPE) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.rv_hospital_equipment_header, parent, false);
            return new HospitalEquipmentHeaderViewHolder(itemView);
        } else if (viewType == EQUIPMENT_VIEW_TYPE) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.rv_hospital_equipment_item, parent, false);
            return new HospitalEquipmentViewHolder(itemView);
        } else {
            return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == HEADER_VIEW_TYPE) {
            HospitalEquipmentHeaderViewHolder headerViewHolder = (HospitalEquipmentHeaderViewHolder) holder;
            headerViewHolder.setHospitalName(hospitalName);
        } else if (holder.getItemViewType() == EQUIPMENT_VIEW_TYPE) {
            HospitalEquipmentViewHolder equipmentViewHolder = (HospitalEquipmentViewHolder) holder;
            // position - 1 to account for the header
            HospitalEquipment hospitalEquipment = hospitalEquipmentList.get(position - 1);
            equipmentViewHolder.setNameText(hospitalEquipment.getEquipmentName());
            equipmentViewHolder.setCountText(hospitalEquipment.getValue());
        }
    }

    @Override
    public int getItemCount() {
        return hospitalEquipmentList.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return HEADER_VIEW_TYPE;
        } else {
            return EQUIPMENT_VIEW_TYPE;
        }
    }


    static class HospitalEquipmentViewHolder extends RecyclerView.ViewHolder {
        TextView nameTV;
        TextView valueTV;

        private HospitalEquipmentViewHolder(View itemView) {
            super(itemView);
            nameTV = itemView.findViewById(R.id.equipment_name);
            valueTV = itemView.findViewById(R.id.equipment_value);
        }

        private void setNameText(String name) {
            nameTV.setText(name);
        }

        private void setCountText(String count) {
            valueTV.setText(count);
        }
    }

    static class HospitalEquipmentHeaderViewHolder extends RecyclerView.ViewHolder {
        ImageView mapIV;
        TextView hospitalNameTV;

        private HospitalEquipmentHeaderViewHolder(View itemView) {
            super(itemView);
            mapIV = itemView.findViewById(R.id.map_image);
            hospitalNameTV = itemView.findViewById(R.id.hospital_name);
        }

        private void setHospitalName(String hospitalName) {
            hospitalNameTV.setText(hospitalName);
        }

        private void setMapImage() {
        }
    }
}

