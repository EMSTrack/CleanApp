package org.emstrack.hospital.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

//import org.emstrack.hospital.CustomDialog;
import org.emstrack.hospital.interfaces.DataListener;
import org.emstrack.hospital.R;
import org.emstrack.hospital.dialogs.ToggleDialog;
import org.emstrack.hospital.dialogs.ValueDialog;
import org.emstrack.models.HospitalEquipment;

import java.util.ArrayList;

/**
 * Created by Fabian Choi on 5/16/2017.
 */
public class ListAdapter extends ArrayAdapter<HospitalEquipment> {

    private final Context context;
    private ArrayList<HospitalEquipment> objects;
    private AlertDialog.Builder alertBuilder;
    private FragmentManager fragmentManager;
    private DataListener dr;

    public ListAdapter(Context context, ArrayList<HospitalEquipment> objects, FragmentManager fm) {
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
        final HospitalEquipment equipmentItem = objects.get(position);

        LayoutInflater inflater = LayoutInflater.from(context);

        final View row;

        Character equipmentEtype = equipmentItem.getEquipmentEtype();

        try {
            // Integer type
            if (equipmentEtype == 'I') {
                System.out.println("Adding Value to List");
                row = inflater.inflate(R.layout.list_item_value, parent, false);

                System.out.println("Grabbing Elements from Row");
                // Grab the elements of the Value ListItem
                TextView text = (TextView) row.findViewById(R.id.valueTextView);
                TextView value = (TextView) row.findViewById(R.id.valueData);

                System.out.println("Setting Elements in Row");
                // Set the elements of the ListItem
                text.setText(equipmentItem.getEquipmentName());
                value.setText(equipmentItem.getValue() + "");

                System.out.println("Setting row onClick Listener");
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        System.out.println("Value ListItem onClick");

                        String title = ((TextView) row.findViewById(R.id.valueTextView)).getText().toString();
                        String message = "¿Cuántas unidades hay disponibles?";
                        String value = ((TextView) row.findViewById(R.id.valueData)).getText().toString();

                        ValueDialog vd = ValueDialog.newInstance(title, message, value);
                        vd.setOnDataChangedListener(dr);
                        vd.show(fragmentManager, "value_dialog");

                        System.out.println("After Show----------------");

                    }
                });

            } else if (equipmentEtype == 'B') {
                System.out.println("Adding Toggle to List");
                row = inflater.inflate(R.layout.list_item_toggle, parent, false);

                // Grab the elements of the Toggle ListItem
                TextView text = (TextView) row.findViewById(R.id.toggleTextView);
                ImageView image = (ImageView) row.findViewById(R.id.toggleImage);

                // Set the elements of the ListItem
                text.setText(equipmentItem.getEquipmentName());
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        System.out.println("Toggle ListItem onClick");

                        String title = ((TextView) row.findViewById(R.id.toggleTextView)).getText().toString();
                        String message = "¿Está disponible este recurso?";
                        String value = equipmentItem.getValue();
                        boolean toggled = (value.equals("True"));

                        ToggleDialog td = ToggleDialog.newInstance(title, message, toggled, value);
                        td.setOnDataChangedListener(dr);

                        td.show(fragmentManager, "toggle_dialog");
                    }
                });

                // Check which image to set
                if (equipmentItem.getValue().equals("True")) {
                    image.setImageResource(R.drawable.green_check);
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
