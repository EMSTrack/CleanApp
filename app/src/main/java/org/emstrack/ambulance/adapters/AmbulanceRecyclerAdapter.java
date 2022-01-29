package org.emstrack.ambulance.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.views.AmbulanceViewHolder;
import org.emstrack.models.Ambulance;

import java.util.List;

/**
 * RecyclerView for ambulances
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class AmbulanceRecyclerAdapter extends RecyclerView.Adapter<AmbulanceViewHolder> {

    public interface LoginAmbulance {
        void login(int id);
    }

    private static final String TAG = AmbulanceRecyclerAdapter.class.getSimpleName();

    private final Activity activity;
    private final LoginAmbulance loginAmbulance;
    private final List<Ambulance> ambulances;

    public AmbulanceRecyclerAdapter(@NonNull Activity activity, @NonNull List<Ambulance> ambulances, @Nullable LoginAmbulance loginAmbulance) {
        this.activity = activity;
        this.ambulances = ambulances;
        this.loginAmbulance = loginAmbulance;
    }

    @NonNull
    @Override
    public AmbulanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ambulance_item, parent, false);
        return new AmbulanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AmbulanceViewHolder holder, int position) {

        Ambulance item = ambulances.get(position);
        holder.setAmbulance(item, activity, loginAmbulance);

    }

    @Override
    public int getItemCount() {
        return ambulances.size();
    }

}

