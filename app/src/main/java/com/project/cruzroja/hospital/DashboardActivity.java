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
//    private Hospital hospital;
    private boolean backPressed;

    public static Hospital selectedHospital;

    private ArrayList<DashboardItem> dashboardItems = new ArrayList<>();
    private ListAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        System.out.println("DashboardActivity OnCreate");
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_dashboard);

        System.out.println("Selected Hospital: " + selectedHospital.getName());

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

        // Get data from Login and place it into the hospital
//        hospital = new Hospital();
//        Intent intent = getIntent();
//        hospital.setId(intent.getIntExtra("hospital_id", 0));
//        hospital.setName(intent.getStringExtra("hospital_name"));

        // Convert Hospital Equipment into DashboardItems
//        for (Equipment equipment : selectedHospital.getEquipment()) {
//            dashboardItems.add(parseEquipment(equipment));
//        }

        // Set adapter
        ListView lv = (ListView) findViewById(R.id.dashboardListView);
        adapter = new ListAdapter(this.getApplicationContext(), selectedHospital.getEquipment(),
                getSupportFragmentManager());
        lv.setAdapter(adapter);

        System.out.println("After ListView Adapter is Set");

        // Mqtt
        client = MqttClient.getInstance(this);
        System.out.println("Before Set Call back");
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
                            Log.d(TAG, equipment.getName() + " " + equipment.getQuantity());
                            equipment.setQuantity(Integer.parseInt(text));
                            refreshOrAddItem(equipment.getName(), equipment);
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

        System.out.println("After Set Call Back");

        // Start retrieving data
        client.subscribeToTopic("hospital/" + selectedHospital.getId() + "/metadata");
        System.out.println("End OnCreate");
    }


    /**
     * Add or refresh equipment to the list
     * @param equipmentName Equipment with equipmentName to refresh
     * @param equipment The updated or new equipment to reflect in the ui
     */
    private void refreshOrAddItem(String equipmentName, Equipment equipment) {
        boolean itemExists = false;
        System.out.println("Begin Refresh Or Add Item");
        // Update item
        for (DashboardItem item : dashboardItems) {
            if(item.getTitle().equals(equipmentName)) {
                item.setValue(equipment.getQuantity() + "");
                itemExists = true;
            }
        }

        // Add item if it doesn't exist
        if(!itemExists) {
            String type = "Value";
            if(equipment.isToggleable())
                type = "Toggle";

            DashboardItem object = new DashboardItem(equipment.getName(), type,
                    equipment.getQuantity() + "");
            dashboardItems.add(object);
        }

        adapter.notifyDataSetChanged(); // Update UI
        System.out.println("After Refresh Or Add Item");
    }

    @Override
    public void onBackPressed() {
//        if(!backPressed) {
//            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
//            alertDialogBuilder.setMessage("Are you sure you want to log out?\nPress back again to exit.");
//            alertDialogBuilder.show();
//            backPressed = true;
//        } else {
//            client.disconnect();
            super.onBackPressed();
//        }
    }
}
