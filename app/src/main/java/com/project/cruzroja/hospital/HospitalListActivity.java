package com.project.cruzroja.hospital;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;


import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.project.cruzroja.hospital.Dialogs.LogoutDialog;
import java.util.ArrayList;


/**
 * Created by devinhickey on 5/24/17.
 */

public class HospitalListActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital_list);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.maintitlebar);
        View view = getSupportActionBar().getCustomView();

        ImageButton imageButton= (ImageButton)view.findViewById(R.id.AddBtn);

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // TODO Remove, to be replaced by pulled resources from the server
        ArrayList<String> listObjects = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String hospitalTitle = ("Hospital " + i);
            listObjects.add(hospitalTitle);
        }
        // End TODO

        // Create the Spinner connection
        Spinner hospitalSpinner = (Spinner) findViewById(R.id.hospitalSpinner);
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
                System.out.println("Hospital Submit Button Clicked");
                Intent dashboard = new Intent(HospitalListActivity.this, DashboardActivity.class);
                startActivity(dashboard);
            }
        });

    }  // end onCreate

    @Override
    public void onBackPressed() {
        System.out.println("BackButton Pressed");
        LogoutDialog ld = LogoutDialog.newInstance();
        ld.show(getFragmentManager(), "logout_dialog");
    }

}
