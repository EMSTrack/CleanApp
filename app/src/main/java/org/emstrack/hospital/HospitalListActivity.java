package org.emstrack.hospital;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
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
import org.emstrack.models.HospitalPermission;
import org.emstrack.mqtt.MqttProfileClient;

/**
 * Created by devinhickey on 5/24/17.
 */

public class HospitalListActivity extends AppCompatActivity {

    private static final String TAG = "HospitalListActivity";

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

        // Retrieve hospitals
        final MqttProfileClient profileClient = ((HospitalApp) getApplication()).getProfileClient();
        final List<HospitalPermission> hospitals = profileClient.getProfile().getHospitals();

        // No hospitals associated with this account
        if (hospitals.size() < 1) {
            Toast toast = new Toast(this);
            toast.setText("This account has no hospitals associated with it!");
            toast.setDuration(Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        // Creates string arraylist of hospital names
        Log.d(TAG, "Creating hospital list...");
        ArrayList<String> listObjects = new ArrayList<>();
        for (HospitalPermission hospital : hospitals) {
            Log.d(TAG, "Adding hospital " + hospital.getHospitalName());
            listObjects.add(hospital.getHospitalName());
        }

        // Create the Spinner connection
        final Spinner hospitalSpinner = findViewById(R.id.hospitalSpinner);
        hospitalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
        ArrayAdapter<String> hospitalListAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listObjects);
        hospitalListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set the spinner's adapter
        hospitalSpinner.setAdapter(hospitalListAdapter);

        // Create the hospital button
        Button submitHospitalButton = findViewById(R.id.submitHospitalButton);
        submitHospitalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "HospitalEquipmentMetadata Submit Button Clicked");

                int position = hospitalSpinner.getSelectedItemPosition();
                Log.d(TAG, "Position Selected: " + position);
                HospitalPermission selectedHospital = hospitals.get(position);
                Log.d(TAG, "Selected HospitalEquipmentMetadata: " + selectedHospital.getHospitalName());

                // Set the static list of HospitalEquipment in Dashboard
                Intent intent = new Intent(HospitalListActivity.this, HospitalEquipmentActivity.class);
                intent.putExtra("SELECTED_HOSPITAL_ID", Integer.toString(selectedHospital.getHospitalId()));
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
