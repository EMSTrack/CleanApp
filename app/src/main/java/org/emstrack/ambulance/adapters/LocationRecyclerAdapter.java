package org.emstrack.ambulance.adapters;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.models.SelectLocationType;
import org.emstrack.ambulance.views.LocationViewHolder;
import org.emstrack.models.NamedAddress;

import java.util.List;

/**
 * Connects NamedAddress data to the RecyclerView
 * @author Mauricio de Oliveira
 * @since 1/17/2022
 */

public class LocationRecyclerAdapter extends RecyclerView.Adapter<LocationViewHolder> {

    public interface SelectLocation {
        void selectLocation(SelectLocationType type, NamedAddress location);
    }

    private static final String TAG = LocationRecyclerAdapter.class.getSimpleName();
    private final Activity activity;
    private final SelectLocation selectLocation;
    private final List<? extends NamedAddress> locations;
    private final SelectLocationType type;
    private int selectedPosition;

    public LocationRecyclerAdapter(Activity activity, SelectLocationType type, List<? extends NamedAddress> locations, SelectLocation selectLocation) {
        this.activity = activity;
        this.locations = locations;
        this.selectLocation = selectLocation;
        this.type = type;
        selectedPosition = RecyclerView.NO_POSITION;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.location_item, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        NamedAddress item = locations.get(position);
        holder.setLocation(type, item, activity, (type, location) -> {
            // update selected item
            notifyItemChanged(selectedPosition);
            selectedPosition = holder.getLayoutPosition();
            notifyItemChanged(selectedPosition);

            // perform click
            selectLocation.selectLocation(type, location);
        });
        Log.d(TAG, String.format("> position = %d, selectedPosition = %d", position, selectedPosition));
        holder.itemView.setSelected(position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

}
