package org.emstrack.ambulance.adapters;

import static org.emstrack.ambulance.models.NamedAddressWithDistance.fromNamedAddresses;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.models.NamedAddressWithDistance;
import org.emstrack.ambulance.models.SelectLocationType;
import org.emstrack.ambulance.util.RecyclerAdapterWithSelectedPosition;
import org.emstrack.ambulance.views.LocationViewHolder;
import org.emstrack.models.GPSLocation;
import org.emstrack.models.NamedAddress;

import java.util.List;

/**
 * Connects NamedAddress data to the RecyclerView
 * @author Mauricio de Oliveira
 * @since 1/17/2022
 */

public class LocationRecyclerAdapter extends RecyclerAdapterWithSelectedPosition<NamedAddressWithDistance, LocationViewHolder> {

    public interface OnClick {
        void onClick(@NonNull SelectLocationType type, @NonNull NamedAddress entry);
    }

    private static final String TAG = LocationRecyclerAdapter.class.getSimpleName();

    public LocationRecyclerAdapter(@NonNull SelectLocationType type, @NonNull List<? extends NamedAddress> namedAddresses,
                                   @NonNull GPSLocation target, @Nullable OnClick onClick) {
        super(fromNamedAddresses(namedAddresses, target),
                onClick != null ? entry -> onClick.onClick(type, entry.getNamedAddress()) : null,
                (listEntry, entry) -> listEntry.getNamedAddress() == entry.getNamedAddress());
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.location_item, parent, false);
        return new LocationViewHolder(view);
    }

}
