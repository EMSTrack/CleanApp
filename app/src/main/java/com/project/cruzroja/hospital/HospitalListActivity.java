package com.project.cruzroja.hospital;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * Created by devinhickey on 5/24/17.
 */

public class HospitalListActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital_list);

        Button submitHospitalButton = (Button) findViewById(R.id.submitHospitalButton);
        submitHospitalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("Hospital Submit Button Clicked");
                Intent dashboard = new Intent(HospitalListActivity.this, DashboardActivity.class);
                startActivity(dashboard);
            }
        });
    }

}
