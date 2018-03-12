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
 * Created by mauricio on 3/11/2018.
 */

// TODO: Modify equipment display depending on type
// TODO: Icon has white instead of transparency

public class HospitalViewHolder extends GroupViewHolder {

    ImageView hospitalThumbnailImageView;
    TextView hospitalNameTextView;
    FrameLayout frameLayout;

    public HospitalViewHolder(View itemView) {
        super(itemView);

        hospitalNameTextView = (TextView) itemView.findViewById(R.id.hospital_name);
        hospitalThumbnailImageView = (ImageView) itemView.findViewById(R.id.hospital_thumbnail);
    }

    public void setHospital(Hospital hospital) {
        hospitalNameTextView.setText(hospital.getName());
    }

}