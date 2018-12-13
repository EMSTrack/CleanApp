package org.emstrack.ambulance.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thoughtbot.expandablerecyclerview.ExpandableRecyclerViewAdapter;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.models.HospitalExpandableGroup;
import org.emstrack.ambulance.views.HospitalEquipmentViewHolder;
import org.emstrack.ambulance.views.HospitalViewHolder;
import org.emstrack.models.EquipmentItem;
import org.emstrack.models.Hospital;

import java.util.List;

/**
 * Created by mauricio on 3/11/2018.
 */

public class HospitalExpandableRecyclerAdapter
        extends ExpandableRecyclerViewAdapter<HospitalViewHolder, HospitalEquipmentViewHolder> {

    private static final String TAG = HospitalExpandableRecyclerAdapter.class.getSimpleName();

    public HospitalExpandableRecyclerAdapter(List<? extends ExpandableGroup> groups) {
        super(groups);
    }

    @Override
    public HospitalViewHolder onCreateGroupViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.hospital_item, parent, false);
        return new HospitalViewHolder(view);
    }

    @Override
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

    @Override
    public void onBindGroupViewHolder(HospitalViewHolder holder, int flatPosition,
                                      ExpandableGroup group) {
        Hospital hospital = ((HospitalExpandableGroup) group).getHospital();
        // Log.d(TAG, "Binding hospital '" + hospital.getName() + "'");

        holder.setHospital(hospital);
    }
}

