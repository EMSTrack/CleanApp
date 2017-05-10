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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.project.cruzroja.hospital.data.Hospital;

import org.json.JSONObject;

import java.util.ArrayList;
import android.view.Window;

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
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_dashboard);


        ArrayList<DashboardObject> listObjects = new ArrayList<>();

        // TODO remove

        // Add static elements
        for (int i = 0; i < 12; i++) {
            DashboardObject valObject = new DashboardObject("Object "+ i, "Value", Integer.toString(i));
            DashboardObject toggleObject = new DashboardObject("Object "+ i, "Toggle", Integer.toString(i));
            listObjects.add(valObject);
            listObjects.add(toggleObject);
        }

        // TODO END


        ListView lv = (ListView) findViewById(R.id.dashboardListView);
        ListAdapter adapter = new ListAdapter(this.getApplicationContext(), listObjects);
        lv.setAdapter(adapter);

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


class ListAdapter extends ArrayAdapter<DashboardObject> {
    private final Context ctx;
    private ArrayList<DashboardObject> objects;
    private AlertDialog.Builder alertBuilder;

    ListAdapter(Context context, ArrayList<DashboardObject> objects) {
        super(context, -1, objects);
        System.out.println("Inside ListAdapter Constructor");
        this.ctx = context;
        this.objects = objects;
        this.alertBuilder = new AlertDialog.Builder(ctx);


    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the object, and the type
        // Inflate the correct view based on the type and update the parts of the layout -> set onClicks

        System.out.println("Position: " + position);
        DashboardObject dashboardObject = objects.get(position);

        LayoutInflater inflater = LayoutInflater.from(ctx);

        View row = null;

        try {

            // Differentiate between two object types
            if (dashboardObject.getType().equals("Value")) {
                System.out.println("Adding Value to List");
                row = inflater.inflate(R.layout.list_item_value, parent, false);

                System.out.println("Grabbing Elements from Row");
                // Grab the elements of the Value ListItem
                TextView text = (TextView) row.findViewById(R.id.valueTextView);
                TextView value = (TextView) row.findViewById(R.id.valueData);

                System.out.println("Setting Elements in Row");
                // Set the elements of the ListItem
                text.setText(dashboardObject.getTitle());
                value.setText(dashboardObject.getValue());

                System.out.println("Setting row onClick Listener");
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        System.out.println("Value ListItem onClick");

                        // Set the title to the title of the ListItem
                        alertBuilder.setTitle(((TextView) v.findViewById(R.id.valueTextView)).getText());
                        alertBuilder.setMessage("How many units are available?");

                        // Create the EditText
//                        final EditText valueText = new EditText(ctx);

                        // Create the EditText LayoutParams
//                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
//                                (LinearLayout.LayoutParams.MATCH_PARENT,
//                                        LinearLayout.LayoutParams.MATCH_PARENT);
//                        valueText.setLayoutParams(params);

                        // Check for a value already there
                        //                    if ( !((TextView)v.findViewById(R.id.valueData)).getText().equals("")) {
                        // Set the value of the editText to the current value of the Data
                        // Set the default value to the data value stored in the ListItem
//                        valueText.setText(((TextView) v.findViewById(R.id.valueData)).getText());
                        //                    }

                        AlertDialog alert = alertBuilder.create();

                        // Add the EditText to the AlertDialog
                        //                    alert.setView(valueText);

                        alert.show();

                    }
                });

            } else if (dashboardObject.getType().equals("Toggle")) {
                System.out.println("Adding Toggle to List");
                row = inflater.inflate(R.layout.list_item_toggle, parent, false);

                // Grab the elements of the Toggle ListItem
                TextView text = (TextView) row.findViewById(R.id.toggleTextView);
                ImageView image = (ImageView) row.findViewById(R.id.toggleImage);

                // Set the elements of the ListItem
                text.setText(dashboardObject.getTitle());
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        System.out.println("Toggle ListItem onClick");
                        // Set the title to the title of the ListItem
                        alertBuilder.setTitle(((TextView) v.findViewById(R.id.toggleTextView)).getText());
                        alertBuilder.setMessage("Is this item available?");
                        alertBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                System.out.println("Yes Button Clicked");
                                dialog.dismiss();
                            }
                        });

                        alertBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                System.out.println("No Button Clicked");
                                dialog.dismiss();
                            }
                        });

                        AlertDialog alert = alertBuilder.create();
                        alert.show();

                    }
                });


                image.setImageResource(R.drawable.apple);
            } else {
                 return new View(ctx);
            }

        } catch (Exception e) {
            System.out.println("Caught an Exception");
            return new View(ctx);
        }

        return row;
    }  // end getView

}
