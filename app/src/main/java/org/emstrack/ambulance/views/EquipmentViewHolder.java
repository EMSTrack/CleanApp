package org.emstrack.ambulance.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.emstrack.ambulance.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Holds the Equipment data (called from EquipmentExpandableRecyclerAdapter)
 * @author James Basa
 * @since 2/19/2020
 */

public class EquipmentViewHolder extends RecyclerView.ViewHolder {

    private TextView equipmentNameTextView;
    private String[] equipment_info = new String[3]; //index: 0 = value; 1 = comment, 2 = date updated
    private Spinner spinner;
    private ArrayAdapter<String> spinnerAdapter;

    public EquipmentViewHolder(Context context, View itemView) {
        super(itemView);

        equipmentNameTextView = itemView.findViewById(R.id.equipment_name);
        equipment_info = context.getResources().getStringArray(R.array.equipment_info);

        spinner = itemView.findViewById(R.id.spinner);
        spinnerAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, equipment_info){
            @Override //so each dropdown item can be multiple lines long
            public View getDropDownView(final int position,final View convertView,final ViewGroup parent)
            {
                final View v=super.getDropDownView(position,convertView,parent);
                v.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        ((TextView)v.findViewById(android.R.id.text1)).setSingleLine(false);
                    }
                });
                return v;
            }
        };
        spinnerAdapter.setDropDownViewResource(R.layout.multiline_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

    }

    @SuppressLint("ResourceAsColor")
    public void setEquipmentName(String equipmentName, Character equipmentType, String equipmentValue, Context context) {

        equipmentNameTextView.setText(equipmentName);

        // this only checks values for integer types and changes text color to red if quantity is 0
        if ((equipmentType == 'I') && (Integer.parseInt(equipmentValue) == 0)){
            equipmentNameTextView.setTextColor(context.getResources().getColor(R.color.colorRed));
        }

    }

    public void setEquipmentValue(String equipmentValue) {

        //changing the backing array will update the adapter if notified
        equipment_info[0] = "Value: "+equipmentValue;
        spinnerAdapter.notifyDataSetChanged();

    }

    public void setEquipmentComment(String equipmentDescription) {

        //changing the backing array will update the adapter if notified
        equipment_info[1] = "Comment: "+equipmentDescription;
        spinnerAdapter.notifyDataSetChanged();

    }

    @SuppressLint("SimpleDateFormat")
    public void setEquipmentDateUpdated(Date updatedOn) {

        // the string representation of date according to the chosen pattern
        String pattern = "MM/dd/yyyy HH:mm:ss";
        DateFormat df = new SimpleDateFormat(pattern);

        // Using DateFormat format method we can create a string
        equipment_info[2] = "Updated on: "+df.format(updatedOn);
        //changing the backing array will update the adapter if notified
        spinnerAdapter.notifyDataSetChanged();

    }

}