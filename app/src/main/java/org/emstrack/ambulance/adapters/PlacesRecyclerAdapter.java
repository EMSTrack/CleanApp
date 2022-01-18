package org.emstrack.ambulance.adapters;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.libraries.places.api.model.AutocompletePrediction;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.views.PlacesViewHolder;

import java.util.List;

/**
 * Connects NamedAddress data to the RecyclerView
 * @author Mauricio de Oliveira
 * @since 1/17/2022
 */

public class PlacesRecyclerAdapter extends RecyclerView.Adapter<PlacesViewHolder> {

    public interface SelectPrediction {
        void selectLocation(AutocompletePrediction prediction);
    }

    private static final String TAG = PlacesRecyclerAdapter.class.getSimpleName();
    private final Activity activity;
    private final SelectPrediction selectPrediction;
    private final List<AutocompletePrediction> locations;
    private int selectedPosition;

    public PlacesRecyclerAdapter(Activity activity, List<AutocompletePrediction> locations, SelectPrediction selectPrediction) {
        this.activity = activity;
        this.locations = locations;
        this.selectPrediction = selectPrediction;
        selectedPosition = RecyclerView.NO_POSITION;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    @NonNull
    @Override
    public PlacesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.location_item, parent, false);
        return new PlacesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlacesViewHolder holder, int position) {
        AutocompletePrediction item = locations.get(position);
        holder.setLocation(item, (location) -> {
            // update selected item
            notifyItemChanged(selectedPosition);
            selectedPosition = holder.getLayoutPosition();
            notifyItemChanged(selectedPosition);

            // perform click
            selectPrediction.selectLocation(location);
        });
        holder.itemView.setSelected(position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

}
