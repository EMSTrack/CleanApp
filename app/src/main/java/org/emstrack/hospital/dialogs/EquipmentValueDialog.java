package org.emstrack.hospital.dialogs;

import android.app.Dialog;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.emstrack.hospital.interfaces.DataListener;
import org.emstrack.hospital.R;

/**
 * Created by devinhickey on 6/5/17.
 */

public class EquipmentValueDialog extends DialogFragment {

    private String title;
    private String message;
    private String updatedData = "";
    private String oldData = "";
    private DataListener dr;

    /**
     * Empty Constructor
     */
    public EquipmentValueDialog() {

    }

    /**
     *
     * @param title
     * @param message
     * @return
     */
    public static EquipmentValueDialog newInstance(String title, String message, String data) {
        EquipmentValueDialog vd = new EquipmentValueDialog();

        Bundle args = new Bundle();
        args.putString("Title", title);
        args.putString("Message", message);
        args.putString("Data", data);

        vd.setArguments(args);

        return vd;

    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        title = getArguments().getString("Title");
        message = getArguments().getString("Message");
        oldData = getArguments().getString("Data");

        System.out.println("Title: " + title);
        System.out.println("Message: " + message);
        System.out.println("Data: " + oldData);


        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
        final EditText valueText = new EditText(getActivity().getApplicationContext());

        System.out.println("Value Type, adding box");

        // Set the data elements
        alertBuilder.setTitle(title);
        alertBuilder.setMessage(message);

        // Initialize the Value Text
        valueText.setGravity(Gravity.CENTER_HORIZONTAL);
        valueText.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        valueText.setTextColor(getResources().getColor(R.color.colorPrimaryDark));

        // Set the current data and move the cursor to the end
        valueText.setText(oldData);
        valueText.setSelection(valueText.getText().length());

        // Create the EditText LayoutParams
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
                (LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
        valueText.setLayoutParams(params);

        alertBuilder.setView(valueText);

        alertBuilder.setNeutralButton(getResources().getString(R.string.equipment_integer_button_positive_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.out.println("Update Button Clicked");
                // Update the value
                // Grab the new value
                updatedData = valueText.getText().toString();

                // Update the data
                onDataChanged(title, updatedData);
                dialog.dismiss();
            }
        });

        alertBuilder.setNegativeButton(getResources().getString(R.string.equipment_integer_button_negative_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                updatedData = "";
                dialog.dismiss();
            }
        });

        return alertBuilder.create();

    }

    public void setOnDataChangedListener(DataListener dr) {
        this.dr = dr;
    }

    public void onDataChanged(String name, String data) {
        dr.onDataChanged(name, data);
    }

}
