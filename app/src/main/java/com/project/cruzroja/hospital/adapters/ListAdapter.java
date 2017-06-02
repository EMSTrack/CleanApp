package com.project.cruzroja.hospital.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

//import com.project.cruzroja.hospital.CustomDialog;
import com.project.cruzroja.hospital.DataListener;
import com.project.cruzroja.hospital.dialogs.CustomDialog;
import com.project.cruzroja.hospital.R;
import com.project.cruzroja.hospital.models.Equipment;

import java.util.ArrayList;

/**
 * Created by Fabian Choi on 5/16/2017.
 */
public class ListAdapter extends ArrayAdapter<Equipment> {
    private final Context context;
    private ArrayList<Equipment> objects;
    private AlertDialog.Builder alertBuilder;
    private FragmentManager fragmentManager;
    private DataListener dr;

    public ListAdapter(Context context, ArrayList<Equipment> objects, FragmentManager fm) {
        super(context, -1, objects);
        System.out.println("Inside ListAdapter Constructor");
        this.context = context;
        this.objects = objects;
        this.alertBuilder = new AlertDialog.Builder(this.context);
        this.fragmentManager = fm;
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the object, and the type
        // Inflate the correct view based on the type and update the parts of the layout -> set onClicks

        System.out.println("GETVIEW - Position: " + position);
//        final DashboardItem dashboardItem = objects.get(position);
        final Equipment equipmentItem = objects.get(position);

        LayoutInflater inflater = LayoutInflater.from(context);

        final View row;

        try {
            // Differentiate between two object types
            if (!equipmentItem.isToggleable()) {
                System.out.println("Adding Value to List");
                row = inflater.inflate(R.layout.list_item_value, parent, false);

                System.out.println("Grabbing Elements from Row");
                // Grab the elements of the Value ListItem
                TextView text = (TextView) row.findViewById(R.id.valueTextView);
                TextView value = (TextView) row.findViewById(R.id.valueData);

                System.out.println("Setting Elements in Row");
                // Set the elements of the ListItem
                text.setText(equipmentItem.getName());
                value.setText(equipmentItem.getQuantity());

                System.out.println("Setting row onClick Listener");
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        System.out.println("Value ListItem onClick");

                        String title = ((TextView) row.findViewById(R.id.valueTextView)).getText().toString();
                        String message = "How many units are available?";
                        boolean isToggleable = equipmentItem.isToggleable();
                        String data = ((TextView) row.findViewById(R.id.valueData)).getText().toString();

                        CustomDialog cd = CustomDialog.newInstance(title, message, isToggleable, data);
                        cd.setOnDataChangedListener(dr);
                        cd.show(fragmentManager, "value_dialog");
                        System.out.println("After Show----------------");

                        // Update to the new text value
//                        ((TextView) v.findViewById(R.id.valueData)).setText(cd.getUpdatedData());
                    }
                });

            } else if (equipmentItem.isToggleable()) {
                System.out.println("Adding Toggle to List");
                row = inflater.inflate(R.layout.list_item_toggle, parent, false);

                // Grab the elements of the Toggle ListItem
                TextView text = (TextView) row.findViewById(R.id.toggleTextView);
                ImageView image = (ImageView) row.findViewById(R.id.toggleImage);

                // Set the elements of the ListItem
                text.setText(equipmentItem.getName());
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        System.out.println("Toggle ListItem onClick");

                        String title = ((TextView) row.findViewById(R.id.toggleTextView)).getText().toString();
                        String message = "Is this resource available?";
                        boolean isToggleable = equipmentItem.isToggleable();
                        String data = Integer.toString(equipmentItem.getQuantity());

                        CustomDialog cd = CustomDialog.newInstance(title, message, isToggleable, data);
                        cd.setOnDataChangedListener(dr);
                        cd.show(fragmentManager, "toggle_dialog");
                    }
                });

                // Check which image to set
                if (equipmentItem.getQuantity() == 1) {
                    image.setImageResource(R.drawable.checkmark);
                } else {
                    image.setImageResource(R.drawable.redx);
                }


            } else {
                return new View(context);
            }

        } catch (Exception e) {
            System.out.println("Caught an Exception");
            return new View(context);
        }

        return row;
    }  // end getView

    public void setOnDataChangedListener(DataListener dr) {
        this.dr = dr;
    }
}
