package com.project.cruzroja.hospital;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.gson.Gson;
import com.project.cruzroja.hospital.adapters.ListAdapter;
import com.project.cruzroja.hospital.dialogs.LogoutDialog;
import com.project.cruzroja.hospital.interfaces.DataListener;
import com.project.cruzroja.hospital.models.Equipment;
import com.project.cruzroja.hospital.models.Hospital;

import android.view.Window;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
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

    private int hospitalId;

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
        hospitalId = selectedHospital.getId();

        // Set adapter
        ListView lv = (ListView) findViewById(R.id.dashboardListView);
        adapter = new ListAdapter(this, selectedHospital.getEquipment(),
                getSupportFragmentManager());
        adapter.setOnDataChangedListener(new DataListener() {
            @Override
            public void onDataChanged(String name, String data) {
                Log.d(TAG, "onDataChanged: " + name + "@" + data);
                client.publishMessage("hospital/" + hospitalId + "/equipment/" + name, data);
            }
        });
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
                    Log.d(TAG, "Message arrived: " + text);
                    // Parse to hospital object
                    selectedHospital = new Gson().fromJson(text, Hospital.class);
                    for (Equipment equipment : selectedHospital.getEquipment()) {
                        client.subscribeToTopic("hospital/" + hospitalId + "/equipment/" + equipment.getName());
                    }
                }
                // Update equipment values
                else {
                    for (Equipment equipment : selectedHospital.getEquipment()) {
                        if(topic.contains(equipment.getName())) {
                            // Found item in the hospital equipments object
                            equipment.setQuantity(Integer.parseInt(text));
                            Log.d(TAG, "Message arrived: " +  equipment.getName() + "@" + equipment.getQuantity());
                            refreshOrAddItem(equipment);
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
        client.subscribeToTopic("hospital/" + hospitalId + "/metadata");
    }

    /**
     * Add or refresh equipment to the list
     * @param equipment The updated or new equipment to reflect in the ui
     */
    private void refreshOrAddItem(Equipment equipment) {
        System.out.println("Inside Refresh Or Add Item");
        adapter.clear();
        adapter.addAll(selectedHospital.getEquipment());
        adapter.notifyDataSetChanged(); // Update UI
        System.out.println("After Refresh Or Add Item");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
