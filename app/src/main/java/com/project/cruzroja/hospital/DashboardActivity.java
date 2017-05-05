package com.project.cruzroja.hospital;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Debug;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.project.cruzroja.hospital.data.Hospital;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by devinhickey on 4/20/17.
 * The Dashboard
 */

public class DashboardActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = DashboardActivity.class.getSimpleName();

    private Database db;
    private Hospital hospital;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        /* Initialize */
        db = new Database(this);
        hospital = new Hospital();
        ArrayList<DashboardObject> listObjects = new ArrayList<>();

        /* Get data from database */
        db.requestHospital(1, new ServerCallback() {
            @Override
            public void onSuccess(Hospital result) {
                hospital = result;
                Log.d(TAG, hospital.getEquipments().get(0).getName());
            }

            @Override
            public void onFailure(VolleyError error) {
                Log.e(TAG, error.toString());
            }
        });
    }

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

class ListAdapter extends ArrayAdapter<DashboardObject> {
    private final Context ctx;
    private ArrayList<DashboardObject> objects;

    public ListAdapter(Context context, ArrayList<DashboardObject> objects) {
        super(context, -1, objects);
        System.out.println("Inside ListAdapter Constructor");
        this.ctx = context;
        this.objects = objects;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the object, and the type
        // Inflate the correct view based on the type and update the parts of the layout -> set onClicks

        System.out.println("Position: " + position);
        DashboardObject dashboardObject = objects.get(position);

        LayoutInflater inflater = LayoutInflater.from(ctx);
        // Currently inflating toggle always
        View row = inflater.inflate(R.layout.list_item_toggle, parent);
        TextView text = (TextView) row.findViewById(R.id.toggleTextView);
        ImageView image = (ImageView) row.findViewById(R.id.toggleImage);


        return row;
    }

}
