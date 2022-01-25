package org.emstrack.ambulance.views;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.places.api.model.AutocompletePrediction;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.util.ViewHolderWithSelectedPosition;

/**
 * Holds the location data
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class PlacesViewHolder extends ViewHolderWithSelectedPosition<AutocompletePrediction> {

    private static final String TAG = PlacesViewHolder.class.getSimpleName();

    private final TextView locationNameText;

    public PlacesViewHolder(@NonNull View view) {
        super(view);
        locationNameText = view.findViewById(R.id.locationName);
    }

    @Override
    public void set(@NonNull AutocompletePrediction prediction, @Nullable OnClick<AutocompletePrediction> onClick) {
        super.set(prediction, onClick);

        String location = prediction.getFullText(null).toString();
        Log.d(TAG, "location = " + location);

        // set text
        locationNameText.setText(location);

    }

}