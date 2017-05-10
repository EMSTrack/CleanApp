package com.project.cruzroja.hospital;

import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by devinhickey on 5/10/17.
 */

public class CustomDialog extends DialogFragment {

    private String updatedData = "";
    private String oldData = "";

    /**
     * Empty Constructor
     */
    public CustomDialog() {}


    /**
     *
     * @param title
     * @param message
     * @return
     */
    public static CustomDialog newInstance(String title, String message, String type, String data) {

        CustomDialog cd = new CustomDialog();

        Bundle args = new Bundle();
        args.putString("Title", title);
        args.putString("Message", message);
        args.putString("Type", type);
        args.putString("Data", data);

        cd.setArguments(args);

        return cd;

    }

    public String getUpdatedData() {
        if (updatedData.equals("")) {
            return oldData;
        } else {
            return updatedData;
        }
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String title = getArguments().getString("Title");
        String message = getArguments().getString("Message");
        String type = getArguments().getString("Type");
        String data = getArguments().getString("Data");

        System.out.println("Title: " + title);
        System.out.println("Message: " + message);
        System.out.println("Type: " + type);
        System.out.println("Data: " + data);


        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());

        // Set the data elements
        alertBuilder.setTitle(title);
        alertBuilder.setMessage(message);

        // Check what type of box it is
        if (type.equals("Value")) {
            System.out.println("Value Type, adding box");

            // Save the old Data
            oldData = data;

            // Initialize the Value Text
            EditText valueText = new EditText(getActivity().getApplicationContext());
            valueText.setGravity(Gravity.CENTER_HORIZONTAL);
            valueText.setRawInputType(InputType.TYPE_CLASS_NUMBER);
            valueText.setTextColor(getResources().getColor(R.color.colorPrimaryDark));

            // Set the current data and move the cursor to the end
            valueText.setText(data);
            valueText.setSelection(valueText.getText().length());

            // Create the EditText LayoutParams
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
                    (LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT);
            valueText.setLayoutParams(params);

            alertBuilder.setView(valueText);

        } else {
            System.out.println("Not a Value Type");
            // Add two checkboxes?

            // Create the Button LayoutParams
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
                    (LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);


            Button yesButton = new Button(getActivity().getApplicationContext());
            yesButton.setText("YES");
//            yesButton.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));

            yesButton.setLayoutParams(params);


            Button noButton = new Button(getActivity().getApplicationContext());
            noButton.setText("NO");
            noButton.setLayoutParams(params);
//            noButton.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));


            LinearLayout ll = new LinearLayout(getActivity().getApplicationContext());
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.addView(yesButton);
            ll.addView(noButton);

            alertBuilder.setView(ll);

        }

        alertBuilder.setNeutralButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.out.println("Update Button Clicked");
                dialog.dismiss();
            }
        });

        return alertBuilder.create();

    }

}
