package com.project.cruzroja.hospital;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.daimajia.swipe.util.Attributes;
import com.project.cruzroja.hospital.adapters.SwipeListAdapter;
import com.project.cruzroja.hospital.items.DashboardItem;
import com.project.cruzroja.hospital.models.Hospital;

import java.util.ArrayList;
import android.view.Window;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by devinhickey on 4/20/17.
 * The Dashboard
 */

public class DashboardActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = DashboardActivity.class.getSimpleName();

    private ArrayList<DashboardItem> dashboardItems = new ArrayList<>();
    private FragmentManager fragmentManager;

    private Database db;
    private MqttClient client;
    private Hospital hospital;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_dashboard);



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
        SwipeListAdapter adapter = new SwipeListAdapter(this.getApplicationContext(), dashboardItems,
                getSupportFragmentManager());
        lv.setAdapter(adapter);
        adapter.setMode(Attributes.Mode.Single);

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
                client.subscribeToTopic("hospital/+/config");
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "Connection to broker lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(TAG, "Message received: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "Message sent successfully");
            }
        });

        // OLD STUFF
//        /* Initialize */
//        db = new Database(this);
//        hospital = new Hospital();
//        ArrayList<DashboardObject> listObjects = new ArrayList<>();
//
//        /* Get data from database */
//        db.requestHospital(1, new MqttConnectionCallback() {
//            @Override
//            public void onSuccess(Hospital result) {
//                hospital = result;
//                Log.d(TAG, hospital.getEquipments().get(0).getName());
//            }
//
//            @Override
//            public void onFailure(VolleyError error) {
//                Log.e(TAG, error.toString());
//            }
//        });


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

}  // end DashboardActivity Class
