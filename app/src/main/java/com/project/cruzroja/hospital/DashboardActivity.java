package com.project.cruzroja.hospital;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
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
        //client = MqttClient.getInstance(this);
        //Mqtt mqtt = new Mqtt();

        // OLD STUFF
//        /* Initialize */
//        db = new Database(this);
//        hospital = new Hospital();
//        ArrayList<DashboardObject> listObjects = new ArrayList<>();
//
//        /* Get data from database */
//        db.requestHospital(1, new ServerCallback() {
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
