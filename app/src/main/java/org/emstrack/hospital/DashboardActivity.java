package org.emstrack.hospital;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.emstrack.hospital.adapters.ListAdapter;
import org.emstrack.hospital.dialogs.LogoutDialog;
import org.emstrack.hospital.interfaces.DataListener;

import org.emstrack.mqtt.MqttProfileClient;
import org.emstrack.mqtt.MqttProfileMessageCallback;

import org.emstrack.models.HospitalEquipment;
import org.emstrack.models.HospitalEquipmentMetadata;
import org.emstrack.models.HospitalPermission;


/**
 * Created by devinhickey on 4/20/17.
 * The Dashboard
 */
public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = DashboardActivity.class.getSimpleName();
    private ListAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_dashboard);

        // Action bar
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.maintitlebar);

        View view = getSupportActionBar().getCustomView();
        ImageView imageButton= view.findViewById(R.id.LogoutBtn);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create the logout dialog for the user
                LogoutDialog ld = LogoutDialog.newInstance();
                ld.show(getFragmentManager(), "logout_dialog");
            }
        });

        // Get data from Login and place it into the hospital
        final int hospitalId = Integer.parseInt(getIntent().getStringExtra("SELECTED_HOSPITAL_ID"));

        // Retrieve client
        final MqttProfileClient profileClient = ((HospitalApp) getApplication()).getProfileClient();

        // Set list adapter
        ListView lv = findViewById(R.id.dashboardListView);
        adapter = new ListAdapter(this, new ArrayList<HospitalEquipment>(), getSupportFragmentManager());
        adapter.setOnDataChangedListener(new DataListener() {
            @Override
            public void onDataChanged(String name, String data) {
                Log.d(TAG, "onDataChanged: " + name + "@" + data);
                try {
                    profileClient.publish("user/" + profileClient.getUsername() +
                            "/hospital/" + hospitalId +
                            "/equipment/" + name + "/data", data, 2, false);
                } catch (MqttException e) {
                    Log.d(TAG, "Failed to publish updated equipment");
                }
            }
        });
        lv.setAdapter(adapter);


        try {

            // Start retrieving data
            profileClient.subscribe("hospital/" + hospitalId + "/metadata", 1, new MqttProfileMessageCallback() {

                @Override
                public void messageArrived(String topic, MqttMessage message) {

                    // Parse to hospital metadata
                    GsonBuilder gsonBuilder = new GsonBuilder();
                    gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                    Gson gson = gsonBuilder.create();

                    try {
                        // Subscribe to all hospital equipment topics
                        profileClient.subscribe("hospital/" + hospitalId + "/equipment/+/data", 1, new MqttProfileMessageCallback() {
                            @Override
                            public void messageArrived(String topic, MqttMessage message) {

                                // Parse to hospital equipment
                                GsonBuilder gsonBuilder = new GsonBuilder();
                                gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                                Gson gson = gsonBuilder.create();

                                // Found item in the hospital equipments object
                                HospitalEquipment equipment = gson.fromJson(new String(message.getPayload()), HospitalEquipment.class);
                                refreshOrAddItem(equipment);

                            }
                        });

                        HospitalEquipmentMetadata[] equipmentMetadata = gson.fromJson(new String(message.getPayload()), HospitalEquipmentMetadata[].class);
                        for (HospitalEquipmentMetadata equipment : equipmentMetadata) {
                            // Subscribe without a callback
                            profileClient.subscribe("hospital/" + hospitalId + "/equipment/" + equipment.getName() + "/data", 1, null);
                        }

                    } catch (MqttException e) {
                        Log.d(TAG, "Could no subscribe to hospital equipment topics");
                    }
                }

            });

        } catch (MqttException e) {
            Log.d(TAG, "Could no subscribe to hospital metadata");
        }

    }

    /**
     * Add or refresh equipment to the list
     * @param equipment The updated or new equipment to reflect in the ui
     */
    private void refreshOrAddItem(HospitalEquipment equipment) {
        Log.d(TAG, "Inside Refresh Or Add Item");
        adapter.add(equipment);
        Log.d(TAG, "After Refresh Or Add Item");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
