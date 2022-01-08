package org.emstrack.ambulance.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.AmbulanceRecyclerAdapter;
import org.emstrack.ambulance.models.EquipmentType;
import org.emstrack.ambulance.models.MessageType;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.Ambulance;
import org.emstrack.models.Settings;

import java.util.Map;

/**
 * Holds the ambulance data
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class AmbulanceViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = AmbulanceViewHolder.class.getSimpleName();
    private final Map<String, String> ambulanceCapabilitiesMap;
    private final Map<String, String> ambulanceStatusMap;

    private final View ambulanceDetailView;
    private final TextView ambulanceIdentifierText;

    private final ImageView ambulanceMessageImageView;
    private final ImageView ambulanceEquipmentImageView;
    private final ImageView ambulanceLocationImageView;
    private final ImageView ambulanceLoginImageView;
    private final ImageView ambulanceThumbnail;
    private final TextView ambulanceStatusText;
    private final TextView ambulanceCapabilityText;
    private final TextView ambulanceCommentText;
    private final TextView ambulanceUpdatedOnText;
    private final View ambulanceCommentLabel;

    public AmbulanceViewHolder(Context context, View view) {
        super(view);

        ambulanceIdentifierText = view.findViewById(R.id.ambulanceIdentifierText);

        ambulanceThumbnail = view.findViewById(R.id.ambulanceThumbnail);
        ambulanceStatusText = view.findViewById(R.id.ambulanceStatusText);

        ambulanceDetailView = view.findViewById(R.id.ambulance_detail);

        ambulanceEquipmentImageView = ambulanceDetailView.findViewById(R.id.ambulanceEquipment);
        ambulanceLocationImageView = ambulanceDetailView.findViewById(R.id.ambulanceLocation);
        ambulanceMessageImageView = ambulanceDetailView.findViewById(R.id.ambulanceMessage);
        ambulanceLoginImageView = ambulanceDetailView.findViewById(R.id.ambulanceLogin);

        ambulanceCapabilityText = ambulanceDetailView.findViewById(R.id.capabilityText);
        ambulanceCommentLabel = ambulanceDetailView.findViewById(R.id.commentLabel);
        ambulanceCommentText = ambulanceDetailView.findViewById(R.id.commentText);
        ambulanceUpdatedOnText = ambulanceDetailView.findViewById(R.id.updatedOnText);

        // disable logout
        ambulanceDetailView.findViewById(R.id.ambulanceLogout).setVisibility(View.GONE);

        // set ambulanceCapabilities
        Settings settings = AmbulanceForegroundService.getAppData().getSettings();
        ambulanceCapabilitiesMap = settings.getAmbulanceCapability();
        ambulanceStatusMap = settings.getAmbulanceStatus();

        // set click action
        view.setOnClickListener(view1 -> {
            showDetail();
        });
    }

    public void showDetail() {

        // toggle visibility of the detail view
        if (ambulanceDetailView.getVisibility() == View.VISIBLE) {
            ambulanceDetailView.setVisibility(View.GONE);
        } else {
            ambulanceDetailView.setVisibility(View.VISIBLE);
        }

    }

    public void setAmbulance(Ambulance ambulance, Activity activity, AmbulanceRecyclerAdapter.LoginAmbulance loginAmbulance) {

        MainActivity mainActivity = (MainActivity) activity;

        int ambulanceId = ambulance.getId();
        ambulanceIdentifierText.setText(ambulance.getIdentifier());
        int vectorColor;
        if (ambulance.getClientId() != null) {
            // online
            ambulanceLoginImageView.setVisibility(View.GONE);
            vectorColor = ContextCompat.getColor(activity, R.color.bootstrapSuccess);
        } else {
            // not online
            ambulanceLoginImageView.setVisibility(View.VISIBLE);
            vectorColor = ContextCompat.getColor(activity, R.color.iconColorDark);
        }
        ambulanceThumbnail.setColorFilter(vectorColor, PorterDuff.Mode.SRC_IN);

        // set status
        String status = ambulance.getStatus();
        ambulanceStatusText.setText(ambulanceStatusMap.get(status));
        ambulanceStatusText.setTextColor(mainActivity.getAmbulanceStatusBackgroundColorMap().get(status));

        // set detail
        ambulanceCapabilityText.setText(ambulanceCapabilitiesMap.get(ambulance.getCapability()));
        ambulanceUpdatedOnText.setText(ambulance.getUpdatedOn().toString());

        // set comment
        String comment = ambulance.getComment();
        if (comment != null && !comment.equals("")) {
            ambulanceCommentText.setText(comment);
            ambulanceCommentText.setVisibility(View.VISIBLE);
            ambulanceCommentLabel.setVisibility(View.VISIBLE);
        } else {
            ambulanceCommentText.setVisibility(View.GONE);
            ambulanceCommentLabel.setVisibility(View.GONE);
        }

        // set message click response
        ambulanceMessageImageView.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("type", MessageType.AMBULANCE);
            bundle.putInt("id", ambulanceId);
            mainActivity.navigate(R.id.action_ambulances_to_messages, bundle);
        });

        // set equipment click response
        ambulanceEquipmentImageView.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("type", EquipmentType.AMBULANCE);
            bundle.putInt("id", ambulanceId);
            mainActivity.navigate(R.id.action_ambulances_to_equipment, bundle);
        });

        // set location click response
        ambulanceLocationImageView.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            Location location = ambulance.getLocation().toLocation();
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            bundle.putParcelable("latLng", latLng);
            mainActivity.navigate(R.id.action_ambulances_to_map, bundle);
        });

        // set select click response
        ambulanceLoginImageView.setOnClickListener(v -> {
            loginAmbulance.login(ambulanceId);
        });

    }

}