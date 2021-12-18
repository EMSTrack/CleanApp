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
import org.emstrack.ambulance.models.EquipmentType;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.Ambulance;
import org.emstrack.models.Settings;

import java.util.Map;

/**
 * Holds the hospital data
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class AmbulanceRecyclerViewViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = AmbulanceRecyclerViewViewHolder.class.getSimpleName();
    private final Map<String, String> ambulanceCapabilities;

    private final View ambulanceDetailView;
    private final TextView ambulanceName;

    private final ImageView ambulanceEquipmentImageView;
    private final ImageView ambulanceLocationImageView;
    private final ImageView ambulanceSelectImageView;
    private final ImageView ambulanceThumbnail;


    public AmbulanceRecyclerViewViewHolder(Context context, View view) {
        super(view);

        ambulanceName = view.findViewById(R.id.ambulance_name);

        ambulanceThumbnail = view.findViewById(R.id.ambulanceThumbnail);

        ambulanceEquipmentImageView = view.findViewById(R.id.ambulanceEquipment);
        ambulanceLocationImageView = view.findViewById(R.id.ambulanceLocation);
        ambulanceSelectImageView = view.findViewById(R.id.ambulanceSelect);

        ambulanceDetailView = view.findViewById(R.id.ambulance_detail);

        // set ambulanceCapabilities
        Settings settings = AmbulanceForegroundService.getAppData().getSettings();
        ambulanceCapabilities = settings.getAmbulanceCapability();

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

    public void setAmbulance(Ambulance ambulance, Activity activity) {

        int ambulanceId = ambulance.getId();
        ambulanceName.setText(ambulance.getIdentifier());
        int vectorColor;
        if (ambulance.getClientId() != null) {
            // online
            ambulanceSelectImageView.setVisibility(View.GONE);
            vectorColor = ContextCompat.getColor(activity, R.color.bootstrapSuccess);
        } else {
            // not online
            ambulanceSelectImageView.setVisibility(View.VISIBLE);
            vectorColor = ContextCompat.getColor(activity, R.color.iconColorDark);
        }
        ambulanceThumbnail.setColorFilter(vectorColor, PorterDuff.Mode.SRC_IN);

        // set detail
        ((TextView) ambulanceDetailView.findViewById(R.id.capabilityText)).setText(ambulanceCapabilities.get(ambulance.getCapability()));
        ((TextView) ambulanceDetailView.findViewById(R.id.commentText)).setText(ambulance.getComment());
        ((TextView) ambulanceDetailView.findViewById(R.id.updatedOnText)).setText(ambulance.getUpdatedOn().toString());

        // set equipment click response
        ambulanceEquipmentImageView.setOnClickListener(view -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("type", EquipmentType.AMBULANCE);
            bundle.putInt("id", ambulanceId);
            ((MainActivity) activity).navigate(R.id.action_ambulances_to_equipment, bundle);
        });

        // set location click response
        ambulanceLocationImageView.setOnClickListener(view -> {
            Bundle bundle = new Bundle();
            Location location = ambulance.getLocation().toLocation();
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            bundle.putParcelable("latLng", latLng);
            ((MainActivity) activity).navigate(R.id.action_ambulances_to_map, bundle);
        });

        // set select click response
        ambulanceSelectImageView.setOnClickListener(view -> {
            Bundle bundle = new Bundle();
            bundle.putInt("id", ambulanceId);
            ((MainActivity) activity).navigate(R.id.action_ambulances_to_ambulance, bundle);
        });

    }

}