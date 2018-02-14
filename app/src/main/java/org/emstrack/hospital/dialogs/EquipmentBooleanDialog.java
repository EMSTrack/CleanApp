package org.emstrack.hospital.dialogs;

import android.app.Dialog;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.emstrack.hospital.interfaces.DataListener;
import org.emstrack.hospital.R;

/**
 * Created by devinhickey on 6/5/17.
 */

public class EquipmentBooleanDialog extends DialogFragment {

    private String title;
    private String message;
    private boolean isToggled;
    private String updatedData = "";
    private String oldData = "";
    private DataListener dr;

    /**
     * Empty Constructor
     */
    public EquipmentBooleanDialog() {

    }

    /**
     *
     * @param title
     * @param message
     * @return
     */
    public static EquipmentBooleanDialog newInstance(String title, String message, boolean isToggled, String data) {

        EquipmentBooleanDialog td = new EquipmentBooleanDialog();

        Bundle args = new Bundle();
        args.putString("Title", title);
        args.putString("Message", message);
        args.putBoolean("Toggle", isToggled);
        args.putString("Data", data);

        td.setArguments(args);

        return td;

    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        title = getArguments().getString("Title");
        message = getArguments().getString("Message");
        isToggled = getArguments().getBoolean("Toggle");
        oldData = getArguments().getString("Data");

        System.out.println("Title: " + title);
        System.out.println("Message: " + message);
        System.out.println("Toggle: " + isToggled);
        System.out.println("Data: " + oldData);

        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
//        final LinearLayout ll = new LinearLayout(getActivity().getApplicationContext());
        final Button yesButton = new Button(getActivity().getApplicationContext());
        final Button noButton = new Button(getActivity().getApplicationContext());

        // Set the data elements
        // Check if the resource is available and set the title to account for it
        System.out.println("Data = " + oldData);
        if (oldData.equals("True")) {
            alertBuilder.setTitle((title + " - " + getResources().getString(R.string.equipment_boolean_true)));
        } else {
            alertBuilder.setTitle((title + " - " + getResources().getString(R.string.equipment_boolean_false)));
        }
        alertBuilder.setMessage(message);

        LayoutInflater inflater = LayoutInflater.from(getContext());

        View v = inflater.inflate(R.layout.toggle_dialog, null, false);
        alertBuilder.setView(v.findViewById(R.id.toggleDialogLayout));

        final Switch toggleSwitch = v.findViewById(R.id.availableSwitch);
        final TextView availableText = v.findViewById(R.id.availableText);
        final TextView unavailableText = v.findViewById(R.id.unavailableText);

        toggleSwitch.setChecked(!isToggled);
        toggleText(!isToggled, availableText, unavailableText);

        /**
         * OnClicks for the elements of the dialog
         */

        availableText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSwitch.setChecked(false);
                toggleText(false, availableText, unavailableText);
            }
        });

        unavailableText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSwitch.setChecked(true);
                toggleText(true, availableText, unavailableText);
            }
        });

        toggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleText(isChecked, availableText, unavailableText);
            }
        });

        alertBuilder.setNeutralButton(getResources().getString(R.string.equipment_bolean_button_positive_text),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!toggleSwitch.isChecked()) {
                            updatedData = "True";
                        } else {
                            updatedData = "False";
                        }

                        // Update the data
                        onDataChanged(title, updatedData);
                        dialog.dismiss();
                    }
                });

        alertBuilder.setNegativeButton(getResources().getString(R.string.equipment_bolean_button_negative_text),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updatedData = "";
                        dialog.dismiss();
                    }
                });

        return alertBuilder.create();

    }

    private void toggleText (boolean toggle, final TextView availableText, final TextView unavailableText) {
        if (!toggle) {
            availableText.setTypeface(null, Typeface.BOLD);
            availableText.setTextColor(getResources().getColor(R.color.colorGreen));
            unavailableText.setTextColor(getResources().getColor(R.color.colorGrey));
            unavailableText.setTypeface(null, Typeface.NORMAL);
        } else {
            availableText.setTypeface(null, Typeface.NORMAL);
            unavailableText.setTextColor(getResources().getColor(R.color.colorPrimary));
            availableText.setTextColor(getResources().getColor(R.color.colorGrey));
            unavailableText.setTypeface(null, Typeface.BOLD);
        }
    }

    public void setOnDataChangedListener(DataListener dr) {
        this.dr = dr;
    }

    public void onDataChanged(String name, String data) {
        dr.onDataChanged(name, data);
    }

}
