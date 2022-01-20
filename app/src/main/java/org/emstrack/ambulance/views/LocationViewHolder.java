package org.emstrack.ambulance.views;

import static org.emstrack.ambulance.util.FormatUtils.formatDistance;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.LocationRecyclerAdapter;
import org.emstrack.ambulance.models.NamedAddressWithDistance;
import org.emstrack.ambulance.models.SelectLocationType;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.NamedAddress;

/**
 * Holds the location data
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class LocationViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = LocationViewHolder.class.getSimpleName();
    private final TextView locationNameText;
    private final View view;
    private final TextView locationDistanceText;

    public LocationViewHolder(View view) {
        super(view);
        locationNameText = view.findViewById(R.id.locationName);
        locationDistanceText = view.findViewById(R.id.locationDistance);
        this.view = view;
    }

    public void setLocation(SelectLocationType type, NamedAddressWithDistance location, Activity activity, LocationRecyclerAdapter.SelectLocation selectLocation) {

        locationNameText.setText(location.getNamedAddress().getName());
        locationDistanceText.setText(
                formatDistance(location.getDistance(),
                        AmbulanceForegroundService.getAppData().getSettings().getUnits()));

        // set click listener
        view.setOnClickListener(v -> selectLocation.selectLocation(type, location.getNamedAddress()));
    }

}