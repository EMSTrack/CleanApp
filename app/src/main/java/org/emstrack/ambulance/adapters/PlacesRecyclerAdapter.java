package org.emstrack.ambulance.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.places.api.model.AutocompletePrediction;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.util.RecyclerAdapterWithSelectedPosition;
import org.emstrack.ambulance.util.ViewHolderWithSelectedPosition;
import org.emstrack.ambulance.views.PlacesViewHolder;

import java.util.List;

/**
 * RecyclerView for named addresses
 * @author Mauricio de Oliveira
 * @since 1/17/2022
 */

public class PlacesRecyclerAdapter extends RecyclerAdapterWithSelectedPosition<AutocompletePrediction, PlacesViewHolder> {

    private static final String TAG = PlacesRecyclerAdapter.class.getSimpleName();

    public PlacesRecyclerAdapter(@NonNull List<AutocompletePrediction> list, @Nullable ViewHolderWithSelectedPosition.OnClick<AutocompletePrediction> onClick) {
        super(list, onClick);
    }

    @NonNull
    @Override
    public PlacesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.location_item, parent, false);
        return new PlacesViewHolder(view);
    }

}
