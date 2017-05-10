package com.project.cruzroja.hospital;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import android.view.Window;
/**
 * Created by aarohan on 5/9/17.
 */

public class CustomWindow extends AppCompatActivity {
    protected TextView title;
    protected ImageView icon;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Request for custom title bar
        this.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        //set to your layout file
        setContentView(R.layout.activity_login);
        setContentView(R.layout.activity_dashboard);
        //Set the titlebar layout
        this.getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.maintitlebar);
    }
}
