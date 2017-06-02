package com.project.cruzroja.hospital;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import com.daimajia.swipe.util.Attributes;
import com.google.gson.Gson;
import com.project.cruzroja.hospital.adapters.ListAdapter;
import com.project.cruzroja.hospital.dialogs.LogoutDialog;
import com.project.cruzroja.hospital.models.Equipment;
import com.project.cruzroja.hospital.models.Hospital;

import java.util.ArrayList;
import android.view.Window;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by devinhickey on 4/20/17.
 * The Dashboard
 */
public class DashboardActivity extends AppCompatActivity {
    private static final String TAG = DashboardActivity.class.getSimpleName();

    private MqttClient client;
    public static Hospital selectedHospital; // We know...

    private ListAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        System.out.println("DashboardActivity OnCreate");
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_dashboard);

        // Action bar
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.maintitlebar);

        View view = getSupportActionBar().getCustomView();
        ImageView imageButton= (ImageView)view.findViewById(R.id.LogoutBtn);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create the logout dialog for the user
                LogoutDialog ld = LogoutDialog.newInstance();
                ld.show(getFragmentManager(), "logout_dialog");
            }
        });

        // Set adapter
        ListView lv = (ListView) findViewById(R.id.dashboardListView);
        adapter = new ListAdapter(this, selectedHospital.getEquipment(),
                getSupportFragmentManager());
        lv.setAdapter(adapter);

        // Mqtt
        client = MqttClient.getInstance(this);
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if(reconnect)
                    Log.d(TAG, "Reconnected to broker");
                else
                    Log.d(TAG, "Connected to broker");
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "Connection to broker lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String text = new String(message.getPayload());
                Log.d(TAG, "Received data " + text + " at topic " + topic);
                // Message from receiving metadata; subscribe to equipments
                if (topic.contains("metadata")) {
                    Log.d(TAG, text);
                    // Parse to hospital object
                    selectedHospital = new Gson().fromJson(text, Hospital.class);
                    for (Equipment equipment : selectedHospital.getEquipment()) {
                        client.subscribeToTopic("hospital/1/equipment/" + equipment.getName());
                    }
                }
                // Update equipment values
                else {
                    for (Equipment equipment : selectedHospital.getEquipment()) {
                        if(topic.contains(equipment.getName())) {
                            // Found item in the hospital equipments object
                            equipment.setQuantity(Integer.parseInt(text));
                            Log.d(TAG, equipment.getName() + " " + equipment.getQuantity());
                            refreshOrAddItem(equipment.getName(), equipment);
                            break; // Break since we found the equipment
                        }
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "Message sent successfully");
            }
        });

        // Start retrieving data
        client.subscribeToTopic("hospital/" + selectedHospital.getId() + "/metadata");
    }


    /**
     * Add or refresh equipment to the list
     * @param equipmentName Equipment with equipmentName to refresh
     * @param equipment The updated or new equipment to reflect in the ui
     */
    private void refreshOrAddItem(String equipmentName, Equipment equipment) {
        boolean itemExists = false;
        // Update item
        // Run through the current list of elements and update any previous ones
        for(int i = 0; i < selectedHospital.getEquipment().size(); i++) {
            Equipment currentEquipment = selectedHospital.getEquipment().get(i);
            // If there is a match, replace the old equipment object with the new one
            if(currentEquipment.getName().equals(equipmentName)) {
                selectedHospital.getEquipment().set(i, equipment);
                itemExists = true;
            }
        }

        // If it doesn't exist then add the equipment to the list
        if(!itemExists) {
            selectedHospital.getEquipment().add(equipment);
        }
        adapter.clear();
        adapter.addAll(selectedHospital.getEquipment());
        adapter.notifyDataSetChanged(); // Update UI
    }

    @Override
    public void onBackPressed() {
            super.onBackPressed();
    }
}
