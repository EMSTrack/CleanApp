package org.emstrack.ambulance.views;

import android.text.SpannableString;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.libraries.places.api.model.AutocompletePrediction;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.PlacesRecyclerAdapter;

/**
 * Holds the location data
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class PlacesViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = PlacesViewHolder.class.getSimpleName();
    private final TextView locationNameText;
    private final View view;

    public PlacesViewHolder(View view) {
        super(view);
        locationNameText = view.findViewById(R.id.locationName);
        this.view = view;
    }

    public void setLocation(AutocompletePrediction prediction, PlacesRecyclerAdapter.SelectPrediction selectPrediction) {

        String location = prediction.getFullText(null).toString();
        Log.d(TAG, "location = " + location);

        // set text
        locationNameText.setText(location);

        // set click listener
        view.setOnClickListener(v -> selectPrediction.selectLocation(prediction));
    }

}