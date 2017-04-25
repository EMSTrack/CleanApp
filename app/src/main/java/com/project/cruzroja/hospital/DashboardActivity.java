package com.project.cruzroja.hospital;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * Created by devinhickey on 4/20/17.
 */

public class DashboardActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Create onClicks for the Buttons
        Button bedsButton = (Button) findViewById(R.id.bedsButton);
        Button roomsButton = (Button) findViewById(R.id.roomsButton);
        Button xrayButton = (Button) findViewById(R.id.xrayButton);
        Button catScanButton = (Button) findViewById(R.id.catScanButton);

        bedsButton.setOnClickListener(this);
        roomsButton.setOnClickListener(this);
        xrayButton.setOnClickListener(this);
        catScanButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        System.out.println("View was Clicked");

        switch(v.getId()) {
            case R.id.bedsButton:
                System.out.println("Beds Button Clicked");
                break;

            case R.id.roomsButton:
                System.out.println("Rooms Button Clicked");
                break;

            case R.id.xrayButton:
                System.out.println("XRAY Button Clicked");
                break;

            case R.id.catScanButton:
                System.out.println("CATScan Button Clicked");
                break;

            default:
                System.out.println("DEFAULT View Clicked");
                break;

        }

    }

}
