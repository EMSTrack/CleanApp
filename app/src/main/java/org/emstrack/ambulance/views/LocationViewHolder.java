package org.emstrack.ambulance.views;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.LocationRecyclerAdapter;
import org.emstrack.ambulance.models.SelectLocationType;
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

    public LocationViewHolder(View view) {
        super(view);
        locationNameText = view.findViewById(R.id.locationName);
        this.view = view;
    }

    public void setLocation(SelectLocationType type, NamedAddress location, Activity activity, LocationRecyclerAdapter.SelectLocation selectLocation) {

        MainActivity mainActivity = (MainActivity) activity;

        locationNameText.setText(location.getName());

        // set click listener
        view.setOnClickListener(v -> selectLocation.selectLocation(type, location));
    }

}