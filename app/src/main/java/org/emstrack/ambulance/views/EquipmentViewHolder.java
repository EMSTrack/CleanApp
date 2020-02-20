package org.emstrack.ambulance.views;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.thoughtbot.expandablerecyclerview.viewholders.GroupViewHolder;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.models.HospitalExpandableGroup;
import org.emstrack.models.Hospital;

/**
 * Created by James on 2/19/2020.
 */

// TODO: change hospital code to equipment code

public class EquipmentViewHolder extends GroupViewHolder {

    ImageView equipmentThumbnailImageView;
    TextView equipmentNameTextView;
    FrameLayout frameLayout;

    public EquipmentViewHolder(View itemView) {
        super(itemView);

        equipmentNameTextView = (TextView) itemView.findViewById(R.id.equipment_name);
        equipmentThumbnailImageView = (ImageView) itemView.findViewById(R.id.equipment_thumbnail);
    }

    public void setEquipment(Equipment equipment) {
        equipmentNameTextView.setText(equipment.getName());
    }

}