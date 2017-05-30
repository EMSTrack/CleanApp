package com.project.cruzroja.hospital;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v4.app.ShareCompat;
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
import com.project.cruzroja.hospital.items.DashboardItem;
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
    private Hospital hospital;

    private ArrayList<DashboardItem> dashboardItems = new ArrayList<>();
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
        ImageButton imageButton= (ImageButton)view.findViewById(R.id.AddBtn);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Get data from Login and place it into the hospital
        hospital = new Hospital();
        Intent intent = getIntent();
        hospital.setId(intent.getIntExtra("hospital_id", 0));
        hospital.setName(intent.getStringExtra("hospital_name"));

        // Set adapter
        ListView lv = (ListView) findViewById(R.id.dashboardListView);
        adapter = new ListAdapter(this.getApplicationContext(), dashboardItems,
                getSupportFragmentManager());
        lv.setAdapter(adapter);

        // Mqtt
        client = MqttClient.getInstance(this);
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if(reconnect) {
                    Log.d(TAG, "Reconnected to broker");
                } else {
                    Log.d(TAG, "Connected to broker");
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "Connection to broker lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String text = new String(message.getPayload());
                // Message from receiving metadata; subscribe to equipments
                if (topic.contains("metadata")) {
                    Log.d(TAG, text);
                    // Parse to hospital object
                    hospital = new Gson().fromJson(text, Hospital.class);
                    for (Equipment equipment : hospital.getEquipments()) {
                        client.subscribeToTopic("hospital/1/equipment/" + equipment.getName());
                    }
                }
                // Update equipment values
                else {
                    for (Equipment equipment : hospital.getEquipments()) {
                        if(topic.contains(equipment.getName())) {
                            // Found item in the hospital equipments object
                            Log.d(TAG, equipment.getName() + " " + equipment.getQuantity());
                            equipment.setQuantity(Integer.parseInt(text));
                            refreshOrAddItem(equipment.getName());
                        }
                        break;
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "Message sent successfully");
            }
        });

        // Start retrieving data
        client.subscribeToTopic("hospital/" + hospital.getId() + "/metadata");
    }

    /**
     * Add or refresh equipment to the list
     * @param equipmentName Equipment with equipmentName to refresh
     */
    private void refreshOrAddItem(String equipmentName) {
        boolean itemExists = false;
        // Update item
        for (DashboardItem item : dashboardItems) {
            if(item.getTitle().equals(equipmentName)) {
                Equipment equipment = getEquipment(equipmentName);
                if(equipment != null) {
                    item.setValue(equipment.getQuantity() + "");
                    itemExists = true;
                }
            }
        }

        // Add item if it doesn't exist
        if(!itemExists) {
            Equipment equipment = getEquipment(equipmentName);

            // Quit if equipment doesn't exist (likely to never happen)
            if(equipment == null)
                return;

            String type = "Value";
            if(equipment.isToggleable())
                type = "Toggle";

            DashboardItem object = new DashboardItem(equipment.getName(), type,
                    equipment.getQuantity() + "");
            dashboardItems.add(object);
        }

        adapter.notifyDataSetChanged(); // Update UI
    }

    /**
     * Get equipment from hospital object given a name
     * @param name Name of the equipment
     * @return Equipment or null
     */
    private Equipment getEquipment(String name) {
        for (Equipment item : hospital.getEquipments()) {
            if(item.getName().equals(name)) {
                return item;
            }
        }
        return null;
    }
}
