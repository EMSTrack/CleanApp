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
        setContentView(R.layout.dashboard);

        // Create onClicks for the Buttons
        Button btn1 = (Button) findViewById(R.id.button1);
        Button btn2 = (Button) findViewById(R.id.button2);
        Button btn3 = (Button) findViewById(R.id.button3);
        Button btn4 = (Button) findViewById(R.id.button4);

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
        btn4.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        System.out.println("View was Clicked");

        switch(v.getId()) {
            case R.id.button1:
                System.out.println("Button 1 Clicked");
                break;

            case R.id.button2:
                System.out.println("Button 2 Clicked");
                break;

            case R.id.button3:
                System.out.println("Button 3 Clicked");
                break;

            case R.id.button4:
                System.out.println("Button 4 Clicked");
                break;

        }

    }

}
