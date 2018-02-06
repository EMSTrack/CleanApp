package org.emstrack.hospital;

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

import org.emstrack.hospital.dialogs.LogoutDialog;
import org.emstrack.hospital.models.HospitalPermission;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by devinhickey on 5/24/17.
 */

public class HospitalListActivity extends AppCompatActivity {

    // TODO instantiate this just in case?
    public static List<HospitalPermission> hospitalList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital_list);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.maintitlebar);
        View view = getSupportActionBar().getCustomView();

        ImageView imageButton = (ImageView) view.findViewById(R.id.LogoutBtn);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogoutDialog ld = LogoutDialog.newInstance();
                ld.show(getFragmentManager(), "logout_dialog");
            }
        });

        // No hospitals associated with this account
        if (hospitalList.size() < 1) {
            Toast toast = new Toast(this);
            toast.setText("This account has no hospitals associated with it!");
            toast.setDuration(Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        // Creates string arraylist of hospital names
        ArrayList<String> listObjects = new ArrayList<>();
        for (int i = 0; i < hospitalList.size(); i++) {
            listObjects.add(hospitalList.get(i).getHospitalName());
        }

        // Create the Spinner connection
        final Spinner hospitalSpinner = (Spinner) findViewById(R.id.hospitalSpinner);
        hospitalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                System.out.println("OnItemSelected Called");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                System.out.println("OnNothingSelected Called");
            }
        });

        // Create the basic adapter
        ArrayAdapter<String> hospitalListAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, listObjects);
        hospitalListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Set the spinner's adapter
        hospitalSpinner.setAdapter(hospitalListAdapter);

        // Create the hospital button
        Button submitHospitalButton = (Button) findViewById(R.id.submitHospitalButton);
        submitHospitalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("HospitalEquipmentMetadata Submit Button Clicked");

                int position = hospitalSpinner.getSelectedItemPosition();
                System.out.println("Position Selected: " + position);
                HospitalPermission selectedHospital = hospitalList.get(position);
                System.out.println("Selected HospitalEquipmentMetadata: " + selectedHospital.getHospitalName());

                // Set the static list of HospitalEquipment in Dashboard
                Intent dashboard = new Intent(HospitalListActivity.this, DashboardActivity.class);
                DashboardActivity.selectedHospital = selectedHospital;
                startActivity(dashboard);
            }
        });

    }  // end onCreate

    @Override
    public void onBackPressed() {
        LogoutDialog ld = LogoutDialog.newInstance();
        ld.show(getFragmentManager(), "logout_dialog");
    }

}
