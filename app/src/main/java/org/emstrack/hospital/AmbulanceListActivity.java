package org.emstrack.hospital;

import java.util.ArrayList;
import java.util.List;

import android.content.DialogInterface;
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

import org.emstrack.hospital.dialogs.LogoutDialog;
import org.emstrack.models.AmbulancePermission;
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
        setContentView(R.layout.activity_hospital_list);

        // Action bar
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.maintitlebar);
        View view = getSupportActionBar().getCustomView();

        // Connect logout dialog
        ImageView imageButton = view.findViewById(R.id.LogoutBtn);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogoutDialog ld = LogoutDialog.newInstance();
                ld.show(getFragmentManager(), "logout_dialog");
            }
        });

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
        ArrayList<String> listObjects = new ArrayList<>();
        for (AmbulancePermission ambulance : ambulances) {
            Log.d(TAG, "Adding ambulance " + ambulance.getAmbulanceIdentifier());
            listObjects.add(ambulance.getAmbulanceIdentifier());
        }

        // Create the Spinner connection
        final Spinner ambulanceSpinner = findViewById(R.id.hospitalSpinner);
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
                android.R.layout.simple_spinner_item, listObjects);
        ambulanceListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set the spinner's adapter
        ambulanceSpinner.setAdapter(ambulanceListAdapter);

        // Create the ambulance button
        Button submitAmbulanceButton = findViewById(R.id.submitHospitalButton);
        submitAmbulanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "AmbulanceEquipmentMetadata Submit Button Clicked");

                int position = ambulanceSpinner.getSelectedItemPosition();
                Log.d(TAG, "Position Selected: " + position);

                AmbulancePermission selectedAmbulance = ambulances.get(position);
                Log.d(TAG, "Selected AmbulanceEquipmentMetadata: " + selectedAmbulance.getAmbulanceIdentifier());

                int ambulanceId = selectedAmbulance.getAmbulanceId();

                // START GPS
                AlertDialog alertDialog = new AlertDialog.Builder(AmbulanceListActivity.this).create();
                alertDialog.setTitle("GPS");
                alertDialog.setMessage("Start GPS");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,
                        getResources().getString(R.string.alert_button_positive_text),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();

            }
        });

    }  // end onCreate

    @Override
    public void onBackPressed() {
        LogoutDialog ld = LogoutDialog.newInstance();
        ld.show(getFragmentManager(), "logout_dialog");
    }

}
