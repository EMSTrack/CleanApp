package org.emstrack.ambulance.adapters;

import android.app.Activity;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.views.AmbulanceRecyclerViewViewHolder;
import org.emstrack.models.Ambulance;

/**
 * Connects Equipment data to the RecyclerView (called from EquipmentFragment)
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class AmbulancesRecyclerAdapter extends RecyclerView.Adapter<AmbulanceRecyclerViewViewHolder> {

    private static final String TAG = AmbulancesRecyclerAdapter.class.getSimpleName();
    private final Activity activity;
    SparseArray<Ambulance> ambulances;

    public AmbulancesRecyclerAdapter(Activity activity, SparseArray<Ambulance> ambulances) {
        this.activity = activity;
        this.ambulances = ambulances;
    }

    @NonNull
    @Override
    public AmbulanceRecyclerViewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ambulance_item, parent, false);
        return new AmbulanceRecyclerViewViewHolder(activity, view);
    }

    @Override
    public void onBindViewHolder(@NonNull AmbulanceRecyclerViewViewHolder holder, int position) {

        Ambulance item = ambulances.valueAt(position);
        holder.setAmbulance(item, activity);

    }

    @Override
    public int getItemCount() {
        return ambulances.size();
    }

}

