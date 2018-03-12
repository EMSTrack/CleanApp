package org.emstrack.ambulance;

import java.util.ArrayList;
import java.util.List;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.util.Log;

import org.emstrack.ambulance.dialogs.LogoutDialog;
import org.emstrack.models.AmbulancePermission;
import org.emstrack.models.HospitalPermission;
import org.emstrack.mqtt.MqttProfileClient;

/**
 * Created by devinhickey on 5/24/17.
 */

public class AmbulanceListActivity extends AppCompatActivity {

    private static final String TAG = "AmbulanceListActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ambulance_list);

        // Retrieve ambulances
        final MqttProfileClient profileClient = ((AmbulanceApp) getApplication()).getProfileClient();
        final List<AmbulancePermission> ambulances = profileClient.getProfile().getAmbulances();

        // No ambulances associated with this account
        if (ambulances.size() < 1) {
            Toast toast = new Toast(this);
            toast.setText("This account has no ambulances associated with it!");
            toast.setDuration(Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        // Creates string arraylist of ambulance names
        Log.d(TAG, "Creating ambulance list...");
        ArrayList<String> ambulanceList = new ArrayList<>();
        for (AmbulancePermission ambulance : ambulances) {
            Log.d(TAG, "Adding ambulance " + ambulance.getAmbulanceIdentifier());
            ambulanceList.add(ambulance.getAmbulanceIdentifier());
        }

        // Create the Spinner connection
        final Spinner ambulanceSpinner = findViewById(R.id.ambulanceSpinner);
        ambulanceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "OnItemSelected Called");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "OnNothingSelected Called");
            }
        });

        // Create the basic adapter
        ArrayAdapter<String> ambulanceListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ambulanceList);
        ambulanceListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set the spinner's adapter
        ambulanceSpinner.setAdapter(ambulanceListAdapter);

        // Create the ambulance button
        Button submitAmbulanceButton = findViewById(R.id.submitAmbulanceButton);
        submitAmbulanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "AmbulanceEquipmentMetadata Submit Button Clicked");

                int position = ambulanceSpinner.getSelectedItemPosition();
                Log.d(TAG, "Position Selected: " + position);

                AmbulancePermission selectedAmbulance = ambulances.get(position);
                Log.d(TAG, "Selected AmbulanceEquipmentMetadata: " + selectedAmbulance.getAmbulanceIdentifier());

                int ambulanceId = selectedAmbulance.getAmbulanceId();

                // Set the static list of HospitalEquipment in Dashboard
                Intent intent = new Intent(AmbulanceListActivity.this, MainActivity.class);
                intent.putExtra("SELECTED_AMBULANCE_ID", Integer.toString(selectedAmbulance.getAmbulanceId()));
                startActivity(intent);

            }
        });

    }  // end onCreate

    @Override
    public void onBackPressed() {
        LogoutDialog ld = LogoutDialog.newInstance();
        ld.show(getFragmentManager(), "logout_dialog");
    }

}
