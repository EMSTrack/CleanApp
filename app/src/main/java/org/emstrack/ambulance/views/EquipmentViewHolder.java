package org.emstrack.ambulance.views;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.thoughtbot.expandablerecyclerview.viewholders.GroupViewHolder;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.models.EquipmentItem;

/**
 * Created by James on 2/19/2020. This file holds the Equipment data and is called from EquipmentFragment.
 */


public class EquipmentViewHolder extends GroupViewHolder {

    TextView equipmentNameTextView;
    String[] equipment_info = new String[3];
    Spinner spinner;
    ArrayAdapter<String> spinnerAdapter;


    public EquipmentViewHolder(View itemView, Context context) {
        super(itemView);

        equipmentNameTextView = itemView.findViewById(R.id.equipment_name);
        equipment_info = Resources.getSystem().getStringArray(R.array.equipment_info);

        spinner = itemView.findViewById(R.id.spinner);
        spinnerAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, equipment_info);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
    }

    public void setEquipment(EquipmentItem equipment) {
        equipmentNameTextView.setText(equipment.getEquipmentName());
    }

    // TODO: change color to green or red depending on value
    public void setEquipmentValue(String equipmentValue) {
        /*String string = spinnerAdapter.getItem(0);
        spinnerAdapter.remove(string);
        spinnerAdapter.insert(equipmentValue, 0);
        */

        //changing the backing array will update the adapter if notified
        equipment_info[0] = equipmentValue;
        spinnerAdapter.notifyDataSetChanged();

    }
}