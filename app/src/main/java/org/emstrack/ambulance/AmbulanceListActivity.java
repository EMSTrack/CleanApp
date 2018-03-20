package org.emstrack.ambulance;

import java.util.ArrayList;
import java.util.List;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.util.Log;

import org.emstrack.ambulance.dialogs.LogoutDialog;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulancePermission;
import org.emstrack.mqtt.MqttProfileClient;

/**
 * Created by devinhickey on 5/24/17.
 */

public class AmbulanceListActivity extends AppCompatActivity {

    private static final String TAG = "AmbulanceListActivity";

    private boolean goAhead = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ambulance_list);

        // Retrieve ambulances
        final MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient(this);
        final List<AmbulancePermission> ambulances = profileClient.getProfile().getAmbulances();

        // No ambulances associated with this account
        if (ambulances.size() < 1) {
            Toast.makeText(this,
                    "This account has no ambulances associated with it!",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Create the Spinner
        final Spinner ambulanceSpinner = findViewById(R.id.ambulanceSpinner);

        // Creates string arraylist of ambulance names
        Log.d(TAG, "Creating ambulance list...");
        ArrayList<String> ambulanceList = new ArrayList<>();
        for (AmbulancePermission ambulance : ambulances) {
            Log.d(TAG, "Adding ambulance " + ambulance.getAmbulanceIdentifier());
            ambulanceList.add(ambulance.getAmbulanceIdentifier());
        }

        // Create the adapter
        ArrayAdapter<String> ambulanceListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ambulanceList);
        ambulanceListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set the spinner's adapter
        ambulanceSpinner.setAdapter(ambulanceListAdapter);

        // Any ambulance currently selected?
        final Ambulance ambulance = AmbulanceForegroundService.getAmbulance();
        if (ambulance != null)
            // Set spinner
            ambulanceSpinner.setSelection(ambulanceListAdapter.getPosition(ambulance.getIdentifier()));

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

                // Warn if current ambulance is requesting location updates
                if (ambulance != null && ambulance.getId() != ambulanceId && AmbulanceForegroundService.isRequestingLocationUpdates()) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(AmbulanceListActivity.this);
                    builder.setTitle(R.string.alert_warning_title);
                    builder.setMessage(getResources().getString(R.string.alert_ambulance_switch, ambulance.getIdentifier(), selectedAmbulance.getAmbulanceIdentifier()))
                            .setPositiveButton(R.string.alert_button_positive_text,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            goAhead = true;
                                        }
                                    })
                            .setNegativeButton(R.string.alert_button_negative_text,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            goAhead = false;

                                        }
                                    });

                    // Display dialog
                    goAhead = true;
                    builder.create().show();

                    // Go ahead?
                    if (!goAhead)
                        return;

                }

                // Retrieve ambulance
                Log.i(TAG, "Retrieve ambulances");
                Intent ambulanceIntent = new Intent(AmbulanceListActivity.this, AmbulanceForegroundService.class);
                ambulanceIntent.setAction(AmbulanceForegroundService.Actions.GET_AMBULANCE);
                ambulanceIntent.putExtra("AMBULANCE_ID", ambulanceId);
                startService(ambulanceIntent);

                // Retrieve hospitals
                Log.i(TAG, "Retrieve hospitals");
                Intent hospitalsIntent = new Intent(AmbulanceListActivity.this, AmbulanceForegroundService.class);
                hospitalsIntent.setAction(AmbulanceForegroundService.Actions.GET_HOSPITALS);
                startService(hospitalsIntent);

                // Start MainActivity
                Log.i(TAG, "Start main activity");
                Intent intent = new Intent(AmbulanceListActivity.this,
                        MainActivity.class);
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
