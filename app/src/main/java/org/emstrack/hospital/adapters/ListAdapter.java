package org.emstrack.hospital.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

//import org.emstrack.hospital.CustomDialog;
import org.emstrack.hospital.interfaces.DataListener;
import org.emstrack.hospital.R;
import org.emstrack.hospital.dialogs.EquipmentBooleanDialog;
import org.emstrack.hospital.dialogs.EquipmentValueDialog;
import org.emstrack.models.HospitalEquipment;

import java.util.ArrayList;

/**
 * Created by Fabian Choi on 5/16/2017.
 */
public class ListAdapter extends ArrayAdapter<HospitalEquipment> {

    private final Context context;
    private final ArrayList<HospitalEquipment> objects;
    private final FragmentManager fragmentManager;
    private DataListener dr;

    public ListAdapter(Context context, ArrayList<HospitalEquipment> objects, FragmentManager fm) {
        super(context, -1, objects);
        this.context = context;
        this.objects = objects;
        this.fragmentManager = fm;
    }

    public void setOnDataChangedListener(DataListener dr) {
        this.dr = dr;
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, ViewGroup parent) {

        // Get the object, and the type
        // Inflate the correct view based on the type and update the parts of the layout -> set onClicks

        // TODO: This needs a revamp; the different equipment types should know how to convert to view

        System.out.println("GETVIEW - Position: " + position);
        final HospitalEquipment equipmentItem = objects.get(position);

        LayoutInflater inflater = LayoutInflater.from(context);

        final View row;

        final Character equipmentEtype = equipmentItem.getEquipmentEtype();

        try {

            // Integer type
            if (equipmentEtype == 'I' || equipmentEtype == 'S') {

                System.out.println("Adding Value to List");
                row = inflater.inflate(R.layout.list_item_value, parent, false);

                // Grab the elements of the Value ListItem
                System.out.println("Grabbing Elements from Row");
                TextView text = row.findViewById(R.id.valueTextView);
                TextView value = row.findViewById(R.id.valueData);

                // Set the elements of the ListItem
                System.out.println("Setting Elements in Row");
                text.setText(equipmentItem.getEquipmentName());
                if (equipmentEtype == 'I')
                    value.setText(equipmentItem.getValue());
                else
                    value.setText("...");
                // Store value in a tag
                row.setTag(equipmentItem.getValue());

                System.out.println("Setting row onClick Listener");
                row.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        String title = ((TextView) row.findViewById(R.id.valueTextView)).getText().toString();
                        String value = v.getTag().toString();
                        Character equipmentType =
                                ((TextView) row.findViewById(R.id.valueData)).getText()
                                        .equals("...") ? 'S' : 'I';
                        String message;
                        if (equipmentEtype == 'I')
                            message = context.getResources().getString(R.string.equipment_integer_message);
                        else
                            message = context.getResources().getString(R.string.equipment_string_message);

                        EquipmentValueDialog vd = EquipmentValueDialog.newInstance(title, message, value);
                        vd.setOnDataChangedListener(dr);
                        vd.show(fragmentManager,
                                "integer_dialog");
                    }

                });

            } else if (equipmentEtype == 'B') {

                System.out.println("Adding Toggle to List");
                row = inflater.inflate(R.layout.list_item_boolean, parent, false);

                // Grab the elements of the Toggle ListItem
                TextView text = row.findViewById(R.id.toggleTextView);
                ImageView image = row.findViewById(R.id.toggleImage);

                // Set the elements of the ListItem
                text.setText(equipmentItem.getEquipmentName());
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        String title = ((TextView) row.findViewById(R.id.toggleTextView)).getText().toString();
                        String message = context.getResources().getString(R.string.equipment_boolean_message);
                        String value = equipmentItem.getValue();
                        boolean toggled = (value.equals("True"));

                        EquipmentBooleanDialog td = EquipmentBooleanDialog.newInstance(title, message, toggled, value);
                        td.setOnDataChangedListener(dr);

                        td.show(fragmentManager,
                                "boolean_dialog");
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

}
