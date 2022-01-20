package org.emstrack.ambulance.adapters;

import android.app.Activity;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.models.NamedAddressWithDistance;
import org.emstrack.ambulance.models.SelectLocationType;
import org.emstrack.ambulance.views.LocationViewHolder;
import org.emstrack.models.GPSLocation;
import org.emstrack.models.NamedAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

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
    private final ArrayList<NamedAddressWithDistance> locations;
    private final SelectLocationType type;
    private int selectedPosition;

    public LocationRecyclerAdapter(Activity activity,
                                   SelectLocationType type, List<? extends NamedAddress> locations, GPSLocation target,
                                   SelectLocation selectLocation) {
        this.activity = activity;
        this.selectLocation = selectLocation;
        this.type = type;
        selectedPosition = RecyclerView.NO_POSITION;

        // create sorted list of locations
        this.locations = new ArrayList<NamedAddressWithDistance>();
        for (NamedAddress location: locations) {
            this.locations.add(new NamedAddressWithDistance(location, target));
        }
        Collections.sort(this.locations, new NamedAddressWithDistance.SortAscending());

    }

    public int getPosition(NamedAddress location) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return IntStream.range(0, locations.size())
                    .filter(i -> locations.get(i).getNamedAddress() == location)
                    .findFirst()
                    .orElse(-1);
        } else {
            for (int i = 0; i < locations.size(); i++) {
                if (locations.get(i).getNamedAddress() == location) {
                    return i;
                }
            }
            return -1;
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public void setSelectedPosition(int position) {
        if (selectedPosition != position) {
            notifyItemChanged(selectedPosition);
            selectedPosition = position;
            notifyItemChanged(selectedPosition);
        }
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.location_item, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        NamedAddressWithDistance item = locations.get(position);
        holder.setLocation(type, item, activity, (type, location) -> {
            // update selected item
            setSelectedPosition(holder.getLayoutPosition());

            // perform click
            selectLocation.selectLocation(type, location);
        });
        // Log.d(TAG, String.format("> position = %d, selectedPosition = %d", position, selectedPosition));
        holder.itemView.setSelected(position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

}
