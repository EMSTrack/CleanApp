package com.project.cruzroja.hospital;

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

public class DashboardActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = DashboardActivity.class.getSimpleName();

    private MqttClient client;
    private Hospital hospital;

    private ArrayList<DashboardItem> dashboardItems = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_dashboard);

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


        // TODO remove

        DashboardItem valObject = new DashboardItem("Available Rooms", "Value", "20");
        DashboardItem toggleObject = new DashboardItem("X-RAY", "Toggle", "N");
        DashboardItem val1Object = new DashboardItem("Available Doctors", "Value", "3");
        DashboardItem toggle1Object = new DashboardItem("CAT Scan", "Toggle", "Y");

        dashboardItems.add(valObject);
        dashboardItems.add(toggleObject);
        dashboardItems.add(val1Object);
        dashboardItems.add(toggle1Object);

        // TODO END


        ListView lv = (ListView) findViewById(R.id.dashboardListView);
        ListAdapter adapter = new ListAdapter(this.getApplicationContext(), dashboardItems,
                getSupportFragmentManager());
        lv.setAdapter(adapter);

        // MQTT
        client = MqttClient.getInstance(this);
        client.connect("brian", "cruzroja", new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if(reconnect) {
                    Log.d(TAG, "Reconnected to broker");
                } else {
                    Log.d(TAG, "Connected to broker");
                }
                client.subscribeToTopic("hospital/1/metadata");
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
                            equipment.setQuantity(Integer.parseInt(text));
                            Log.d(TAG, equipment.getName() + " " + equipment.getQuantity());
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

    }  // end onCreate

    @Override
    public void onClick(View v) {
        System.out.println("View was Clicked");

        switch(v.getId()) {

            default:
                System.out.println("DEFAULT View Clicked");
                break;

        }
    }

}
