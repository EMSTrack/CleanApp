package org.emstrack.ambulance.views;

import static org.emstrack.ambulance.util.FormatUtils.formatDistance;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.models.NamedAddressWithDistance;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.util.FormatUtils;
import org.emstrack.ambulance.util.ViewHolderWithSelectedPosition;
import org.emstrack.models.Settings;

/**
 * Holds the location data
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class LocationViewHolder extends ViewHolderWithSelectedPosition<NamedAddressWithDistance> {

    private static final String TAG = LocationViewHolder.class.getSimpleName();

    private final TextView locationNameText;
    private final TextView locationDistanceText;

    public LocationViewHolder(@NonNull View view) {
        super(view);
        locationNameText = view.findViewById(R.id.locationName);
        locationDistanceText = view.findViewById(R.id.locationDistance);
    }

    @Override
    public void set(@NonNull NamedAddressWithDistance location, OnClick<NamedAddressWithDistance> onClick) {
        super.set(location, onClick);

        locationNameText.setText(location.getNamedAddress().getName());
        Settings settings = AmbulanceForegroundService.getAppData().getSettings();
        locationDistanceText.setText(
                formatDistance(location.getDistance(),
                        settings != null ? settings.getUnits() : FormatUtils.METRIC));
    }

}