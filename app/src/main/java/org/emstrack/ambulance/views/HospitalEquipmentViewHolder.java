package org.emstrack.ambulance.views;

import android.view.View;
import android.widget.TextView;

import com.thoughtbot.expandablerecyclerview.viewholders.ChildViewHolder;

import org.emstrack.ambulance.R;
import org.emstrack.models.EquipmentItem;

/**
 * Created by mauricio on 3/11/2018.
 */

public class HospitalEquipmentViewHolder extends ChildViewHolder {

    TextView hospitalEquipmentNameTextView;
    TextView hospitalEquipmentValueTextView;

    public HospitalEquipmentViewHolder(View itemView) {
        super(itemView);

        hospitalEquipmentNameTextView = itemView.findViewById(R.id.hospital_equipment_name);
        hospitalEquipmentValueTextView = itemView.findViewById(R.id.hospital_equipment_value);
    }

    public void setHospitalEquipment(EquipmentItem hospitalEquipment) {
        hospitalEquipmentNameTextView.setText(hospitalEquipment.getEquipmentName());
        hospitalEquipmentValueTextView.setText(hospitalEquipment.getValue());
    }

}
