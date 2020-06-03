package org.emstrack.ambulance.views;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.emstrack.ambulance.R;
import org.emstrack.models.EquipmentItem;
import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Holds the Equipment data (called from EquipmentExpandableRecyclerAdapter)
 * @author James Basa
 * @since 2/19/2020
 */

public class EquipmentViewHolder extends RecyclerView.ViewHolder {

    private final CheckBox equipmentValueCheckbox;
    private final TextView equipmentDetailTextView;
    private final TextView equipmentValueTextView;
    private final TextView equipmentNameTextView;

    private static final String TAG = EquipmentViewHolder.class.getSimpleName();


    public EquipmentViewHolder(Context context, View view) {
        super(view);

        equipmentNameTextView = view.findViewById(R.id.equipment_name);

        equipmentValueTextView = view.findViewById(R.id.equipment_value);
        equipmentValueCheckbox = view.findViewById(R.id.equipment_checkbox);

        equipmentDetailTextView = view.findViewById(R.id.equipment_detail);

        view.setOnClickListener(v -> {
            // toggle visibility of the detail view
            if (equipmentDetailTextView.getVisibility() == View.VISIBLE)
                equipmentDetailTextView.setVisibility(View.GONE);
            else
                equipmentDetailTextView.setVisibility(View.VISIBLE);
        });

    }

    public void setEquipment(EquipmentItem item, Context context) {

        equipmentNameTextView.setText(item.getEquipmentName());
        if (item.isBoolean()) {

            boolean value = item.valueToBoolean();
            equipmentValueTextView.setVisibility(View.GONE);
            equipmentValueCheckbox.setChecked(value);
            equipmentNameTextView.setTextColor(context.getResources().getColor(value ? R.color.bootstrapSuccess : R.color.bootstrapDanger));

        } else {

            equipmentValueTextView.setText(item.valueToString());
            equipmentValueCheckbox.setVisibility(View.GONE);
            try {
                if (item.isInteger() && item.valueToInteger() == 0)
                    equipmentNameTextView.setTextColor(context.getResources().getColor(R.color.bootstrapDanger));
                else
                    equipmentNameTextView.setTextColor(context.getResources().getColor(R.color.bootstrapSuccess));
            } catch (NumberFormatException e) {
                equipmentNameTextView.setTextColor(context.getResources().getColor(R.color.bootstrapDanger));
            }

        }

        equipmentDetailTextView.setText(context.getString(R.string.equipment_detail_text, item.getComment(), item.getUpdatedOn()));

    }

}