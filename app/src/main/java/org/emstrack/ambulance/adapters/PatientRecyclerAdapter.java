package org.emstrack.ambulance.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.views.PatientViewHolder;
import org.emstrack.models.Patient;

import java.util.List;

/**
 * RecyclerView for patients
 * @author Mauricio de Oliveira
 * @since 7/07/2020
 */

public class PatientRecyclerAdapter extends RecyclerView.Adapter<PatientViewHolder> {
    
    private static final String TAG = PatientRecyclerAdapter.class.getSimpleName();

    private final List<Patient> patients;

    public PatientRecyclerAdapter(@NonNull List<Patient> patients) {
        this.patients = patients;
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.patient_item, parent, false);
        return new PatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {

        Patient item = patients.get(position);
        holder.setPatient(item);

    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

}

