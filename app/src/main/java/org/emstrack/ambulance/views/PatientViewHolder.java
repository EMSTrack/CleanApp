package org.emstrack.ambulance.views;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.R;
import org.emstrack.models.Patient;

/**
 * Holds the patient data
 * @author Mauricio de Oliveira
 * @since 1/7/2022
 */

public class PatientViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = PatientViewHolder.class.getSimpleName();
    private final TextView patientNameText;
    private final TextView patientAgeText;

    public PatientViewHolder(View view) {
        super(view);

        patientNameText = view.findViewById(R.id.patientNameText);
        patientAgeText = view.findViewById(R.id.patientAgeText);

    }

    public void setPatient(Patient patient) {

        Integer age = patient.getAge();
        if (age != null && age >= 0) {
            patientAgeText.setText(String.valueOf(age));
        }
        patientNameText.setText(patient.getName());
    }
}