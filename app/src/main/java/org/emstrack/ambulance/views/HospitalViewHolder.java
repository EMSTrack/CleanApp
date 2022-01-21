package org.emstrack.ambulance.views;

import static org.emstrack.ambulance.util.FormatUtils.formatDateTime;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.models.EquipmentType;
import org.emstrack.models.Hospital;

import java.text.DateFormat;

/**
 * Holds the hospital data
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class HospitalViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = HospitalViewHolder.class.getSimpleName();
    private final TextView hospitalNameText;
    private final View hospitalDetailView;
    private final ImageView hospitalEquipmentImageView;
    private final ImageView hospitalLocationImageView;
    private final View hospitalCommentLabel;
    private final TextView hospitalCommentText;
    private final TextView hospitalUpdatedOnText;
    private final TextView hospitalAddressText;

    public HospitalViewHolder(View view) {
        super(view);

        hospitalNameText = view.findViewById(R.id.hospitalNameText);

        hospitalDetailView = view.findViewById(R.id.hospital_detail);

        hospitalEquipmentImageView = hospitalDetailView.findViewById(R.id.hospitalEquipment);
        hospitalLocationImageView = hospitalDetailView.findViewById(R.id.hospitalLocation);

        hospitalCommentLabel = hospitalDetailView.findViewById(R.id.commentLabel);
        hospitalCommentText = hospitalDetailView.findViewById(R.id.commentText);

        hospitalAddressText = hospitalDetailView.findViewById(R.id.addressText);
        hospitalUpdatedOnText = hospitalDetailView.findViewById(R.id.updatedOnText);

        // set click action
        view.setOnClickListener(view1 -> {
            toggleDetail();
        });

    }

    public void toggleDetail() {

        // toggle visibility of the detail view
        if (hospitalDetailView.getVisibility() == View.VISIBLE) {
            hospitalDetailView.setVisibility(View.GONE);
        } else {
            hospitalDetailView.setVisibility(View.VISIBLE);
        }

    }
    
    public void setHospital(Hospital hospital, Activity activity) {

        MainActivity mainActivity = (MainActivity) activity;

        int hospitalId = hospital.getId();

        hospitalNameText.setText(hospital.getName());

        // set detail
        hospitalUpdatedOnText.setText(formatDateTime(hospital.getUpdatedOn(), DateFormat.SHORT));

        // set address
        hospitalAddressText.setText(hospital.toAddress());

        // set comment
        String comment = hospital.getComment();
        if (comment != null && !comment.equals("")) {
            hospitalCommentText.setText(comment);
            hospitalCommentText.setVisibility(View.VISIBLE);
            hospitalCommentLabel.setVisibility(View.VISIBLE);
        } else {
            hospitalCommentText.setVisibility(View.GONE);
            hospitalCommentLabel.setVisibility(View.GONE);
        }

        // set equipment click response
        hospitalEquipmentImageView.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("type", EquipmentType.HOSPITAL);
            bundle.putInt("id", hospitalId);
            mainActivity.navigate(R.id.action_hospitals_to_equipment, bundle);
        });

        // set location click response
        hospitalLocationImageView.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            Location location = hospital.getLocation().toLocation();
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            bundle.putParcelable("latLng", latLng);
            mainActivity.navigate(R.id.action_hospitals_to_map, bundle);
        });

    }

}