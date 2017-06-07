package com.project.cruzroja.hospital.dialogs;

import android.app.Dialog;
import android.graphics.Typeface;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.project.cruzroja.hospital.DataListener;
import com.project.cruzroja.hospital.R;

import org.w3c.dom.Text;

/**
 * Created by devinhickey on 6/5/17.
 */

public class ToggleDialog extends DialogFragment {

    private String title;
    private String message;
    private boolean isToggled;
    private String updatedData = "";
    private String oldData = "";
    private DataListener dr;

    /**
     * Empty Constructor
     */
    public ToggleDialog() {

    }

    /**
     *
     * @param title
     * @param message
     * @return
     */
    public static ToggleDialog newInstance(String title, String message, boolean isToggled, String data) {
        ToggleDialog td = new ToggleDialog();

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
        if (oldData.equals("1")) {
            alertBuilder.setTitle((title + " - Currently Available"));
        } else {
            alertBuilder.setTitle((title + " - Not Currently Available"));
        }
        alertBuilder.setMessage(message);

        LayoutInflater inflater = LayoutInflater.from(getContext());

        View v = inflater.inflate(R.layout.toggle_dialog, null, false);
        alertBuilder.setView(v.findViewById(R.id.toggleDialogLayout));

        final Switch toggleSwitch = (Switch) v.findViewById(R.id.availableSwitch);
        final TextView availableText = (TextView) v.findViewById(R.id.availableText);
        final TextView unavailableText = (TextView) v.findViewById(R.id.unavailableText);

        toggleSwitch.setChecked(!isToggled);
        toggleText(!isToggled, availableText, unavailableText);

        toggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleText(isChecked, availableText, unavailableText);
            }
        });

        alertBuilder.setNeutralButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!toggleSwitch.isChecked()) {
                    updatedData = "1";
                } else {
                    updatedData = "0";
                }

                // Update the data
                onDataChanged(title, updatedData);
                dialog.dismiss();
            }
        });

        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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
