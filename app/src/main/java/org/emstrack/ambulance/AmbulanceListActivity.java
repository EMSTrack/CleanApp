package org.emstrack.ambulance;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import org.emstrack.ambulance.dialogs.AlertSnackbar;
import org.emstrack.ambulance.dialogs.LogoutDialog;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.services.OnServiceComplete;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulancePermission;
import org.emstrack.mqtt.MqttProfileClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by devinhickey on 5/24/17.
 */

public class AmbulanceListActivity extends AppCompatActivity {

    private static final String TAG = AmbulanceListActivity.class.getSimpleName();

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
        Ambulance ambulance = AmbulanceForegroundService.getAmbulance();
        if (ambulance != null)
            // Set spinner
            ambulanceSpinner.setSelection(ambulanceListAdapter.getPosition(ambulance.getIdentifier()));

        // Create the ambulance button
        Button submitAmbulanceButton = findViewById(R.id.submitAmbulanceButton);
        submitAmbulanceButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        // Get selected ambulance
                        int position = ambulanceSpinner.getSelectedItemPosition();
                        final AmbulancePermission selectedAmbulance = ambulances.get(position);
                        Log.d(TAG, "Selected ambulance " + selectedAmbulance.getAmbulanceIdentifier());

                        // Any ambulance currently selected?
                        final Ambulance ambulance = AmbulanceForegroundService.getAmbulance();

                        // Warn if current ambulance is requesting location updates
                        if (ambulance != null) {

                            Log.d(TAG, "Current ambulance " + ambulance.getIdentifier());
                            Log.d(TAG, "Requesting location updates? " +
                                    (AmbulanceForegroundService.isRequestingLocationUpdates() ? "TRUE" : "FALSE"));

                            if (ambulance.getId() == selectedAmbulance.getAmbulanceId()) {

                                // Start MainActivity
                                Log.i(TAG, "Start main activity");
                                Intent intent = new Intent(AmbulanceListActivity.this,
                                        MainActivity.class);
                                startActivity(intent);

                                return;

                            } else // ambulance.getId() != selectedAmbulance.getAmbulanceId()
                                if (AmbulanceForegroundService.isRequestingLocationUpdates()) {

                                // Can't do: stop location updates first!
                                Log.d(TAG, "Switching ambulance during location updates");

                                // Display dialog
                                new AlertSnackbar(AmbulanceListActivity.this)
                                        .alert(getResources().getString(R.string.alert_ambulance_switch, ambulance.getIdentifier()),
                                                new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        // set spinner back to original ambulance
                                                        ambulanceSpinner.setSelection(((ArrayAdapter<String>) ambulanceSpinner.getAdapter()).getPosition(ambulance.getIdentifier()));
                                                    }
                                                });

                                return;

                            }

                        }

                        // otherwise go ahead!
                        retrieveAmbulance(selectedAmbulance);

                    }

                });

    }

    public void retrieveAmbulance(AmbulancePermission selectedAmbulance) {

        // Retrieve ambulance
        Intent ambulanceIntent = new Intent(AmbulanceListActivity.this, AmbulanceForegroundService.class);
        ambulanceIntent.setAction(AmbulanceForegroundService.Actions.GET_AMBULANCE);
        ambulanceIntent.putExtra("AMBULANCE_ID", selectedAmbulance.getAmbulanceId());

        // What to do when GET_AMBULANCE service completes?
        new OnServiceComplete(AmbulanceListActivity.this,
                AmbulanceForegroundService.BroadcastActions.SUCCESS,
                AmbulanceForegroundService.BroadcastActions.FAILURE,
                ambulanceIntent) {

            @Override
            public void onSuccess(Bundle extras) {

                // Retrieve hospitals
                Intent hospitalsIntent = new Intent(AmbulanceListActivity.this, AmbulanceForegroundService.class);
                hospitalsIntent.setAction(AmbulanceForegroundService.Actions.GET_HOSPITALS);

                // What to do when GET_HOSPITALS service completes?
                new OnServiceComplete(AmbulanceListActivity.this,
                        AmbulanceForegroundService.BroadcastActions.SUCCESS,
                        AmbulanceForegroundService.BroadcastActions.FAILURE,
                        hospitalsIntent) {

                    @Override
                    public void onSuccess(Bundle extras) {

                        // Start MainActivity
                        Log.i(TAG, "Start main activity");
                        Intent intent = new Intent(AmbulanceListActivity.this,
                                MainActivity.class);
                        startActivity(intent);

                    }
                }
                        .setFailureMessage(getString(R.string.couldNotRetrieveHospitals))
                        .setAlert(new AlertSnackbar(AmbulanceListActivity.this));

            }

        }
                .setFailureMessage(getResources().getString(R.string.couldNotRetrieveAmbulance, selectedAmbulance.getAmbulanceIdentifier()))
                .setAlert(new AlertSnackbar(AmbulanceListActivity.this));

    }

    @Override
    public void onBackPressed() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle(R.string.alert_warning_title);
        alertDialogBuilder.setMessage(R.string.logout_confirm);

        // Cancel button
        alertDialogBuilder.setNegativeButton(
                R.string.alert_button_negative_text,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        /* do nothing */
                    }
                });

        // Create the OK button that logs user out
        alertDialogBuilder.setPositiveButton(
                R.string.alert_button_positive_text,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Log.i(TAG,"LogoutDialog: OK Button Clicked");

                        // Stop foreground activity
                        Intent intent = new Intent(AmbulanceListActivity.this, AmbulanceForegroundService.class);
                        intent.setAction(AmbulanceForegroundService.Actions.LOGOUT);

                        // What to do when service completes?
                        new OnServiceComplete(AmbulanceListActivity.this,
                                AmbulanceForegroundService.BroadcastActions.SUCCESS,
                                AmbulanceForegroundService.BroadcastActions.FAILURE,
                                intent) {

                            @Override
                            public void onSuccess(Bundle extras) {
                                Log.i(TAG, "onSuccess");

                                // Start login activity
                                Intent loginIntent = new Intent(AmbulanceListActivity.this, LoginActivity.class);
                                loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(loginIntent);

                            }

                        }
                                .setFailureMessage(getString(R.string.couldNotLogout))
                                .setAlert(new AlertSnackbar(AmbulanceListActivity.this));

                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }

}
