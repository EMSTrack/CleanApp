package org.emstrack.ambulance.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.adapters.AmbulanceRecyclerAdapter;
import org.emstrack.ambulance.models.EquipmentType;
import org.emstrack.ambulance.models.MessageType;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.Ambulance;
import org.emstrack.models.Patient;
import org.emstrack.models.Settings;
import org.w3c.dom.Text;

import java.util.Map;

/**
 * Holds the patient data
 * @author Mauricio de Oliveira
 * @since 1/7/2022
 */

public class PatientViewHolder extends RecyclerView.ViewHolder {

    private static final String TAG = PatientViewHolder.class.getSimpleName();
    private final TextView patientNameText;
    private final TextView patientAgeText;

    public PatientViewHolder(Context context, View view) {
        super(view);

        patientNameText = view.findViewById(R.id.patientNameText);
        patientAgeText = view.findViewById(R.id.patientAgeText);

    }

    public void setPatient(Patient patient, Activity activity) {

        MainActivity mainActivity = (MainActivity) activity;

        Integer age = patient.getAge();
        if (age != null && age >= 0) {
            patientAgeText.setText(String.valueOf(age));
        }
        patientNameText.setText(patient.getName());
    }
}