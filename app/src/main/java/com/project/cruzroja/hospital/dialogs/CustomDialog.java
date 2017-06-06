package com.project.cruzroja.hospital.dialogs;

//import android.app.Dialog;
//import android.provider.ContactsContract;
//import android.support.annotation.NonNull;
//import android.support.v4.app.DialogFragment;
//import android.content.DialogInterface;
//import android.os.Bundle;
//import android.support.v7.app.AlertDialog;
//import android.text.InputType;
//import android.util.Log;
//import android.view.Gravity;
//import android.view.View;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.LinearLayout;
//
//import com.project.cruzroja.hospital.DataListener;
//import com.project.cruzroja.hospital.R;
//
///**
// * Created by devinhickey on 5/10/17.
// */
//
//public class CustomDialog extends DialogFragment {
//
//    private String title;
//    private String message;
//    private boolean isToggleable;
//    private String updatedData = "";
//    private String oldData = "";
//    private DataListener dr;
//
//    /**
//     * Empty Constructor
//     */
//    public CustomDialog() {}
//
//    /**
//     *
//     * @param title
//     * @param message
//     * @return
//     */
//    public static CustomDialog newInstance(String title, String message, boolean isToggleable, String data) {
//        CustomDialog cd = new CustomDialog();
//
//        Bundle args = new Bundle();
//        args.putString("Title", title);
//        args.putString("Message", message);
//        args.putBoolean("Toggle", isToggleable);
//        args.putString("Data", data);
//
//        cd.setArguments(args);
//
//        return cd;
//
//    }
//
//    public String getUpdatedData() {
//        if (updatedData.equals("")) {
//            return oldData;
//        } else {
//            return updatedData;
//        }
//    }
//
//
//    @Override
//    @NonNull
//    public Dialog onCreateDialog(Bundle savedInstanceState) {
//        title = getArguments().getString("Title");
//        message = getArguments().getString("Message");
//        isToggleable = getArguments().getBoolean("Toggle");
//        oldData = getArguments().getString("Data");
//
//        System.out.println("Title: " + title);
//        System.out.println("Message: " + message);
//        System.out.println("Toggle: " + isToggleable);
//        System.out.println("Data: " + oldData);
//
//
//        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
//        final LinearLayout ll = new LinearLayout(getActivity().getApplicationContext());
//        final Button yesButton = new Button(getActivity().getApplicationContext());
//        final Button noButton = new Button(getActivity().getApplicationContext());
//        final EditText valueText = new EditText(getActivity().getApplicationContext());
//
//        // Check what type of box it is
//        if (!isToggleable) {
//            System.out.println("Value Type, adding box");
//
//            // Set the data elements
//            alertBuilder.setTitle(title);
//            alertBuilder.setMessage(message);
//
//            // Initialize the Value Text
//            valueText.setGravity(Gravity.CENTER_HORIZONTAL);
//            valueText.setRawInputType(InputType.TYPE_CLASS_NUMBER);
//            valueText.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
//
//            // Set the current data and move the cursor to the end
//            valueText.setText(oldData);
//            valueText.setSelection(valueText.getText().length());
//
//            // Create the EditText LayoutParams
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
//                    (LinearLayout.LayoutParams.MATCH_PARENT,
//                            LinearLayout.LayoutParams.MATCH_PARENT);
//            valueText.setLayoutParams(params);
//
//            alertBuilder.setView(valueText);
//
//        } else {
//            System.out.println("Not a Value Type");
//            // Add two checkboxes?
//
//            // Set the data elements
//            // Check if the resource is available and set the title to account for it
//            System.out.println("Data = " + oldData);
//            if (oldData.equals("1")) {
//                alertBuilder.setTitle((title + " - Currently Available"));
//            } else {
//                alertBuilder.setTitle((title + " - Not Currently Available"));
//            }
//            alertBuilder.setMessage(message);
//
//
//            // Create the Button LayoutParams
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
//                    (LinearLayout.LayoutParams.WRAP_CONTENT,
//                            LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
//
//
//
//
//            // Set YesButton traits
//            yesButton.setText("YES");
//            yesButton.setTag("yesButton");
//            yesButton.setHighlightColor(getResources().getColor(R.color.colorPrimaryDark));
//            yesButton.setLayoutParams(params);
//            yesButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    System.out.println("Yes OnClick");
//                    noButton.setSelected(false);
//                    yesButton.setSelected(true);
//                }
//            });
//
//            // Set NoButton traits
//            noButton.setText("NO");
//            noButton.setTag("noButton");
//            noButton.setHighlightColor(getResources().getColor(R.color.colorPrimaryDark));
//            noButton.setLayoutParams(params);
//            noButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    System.out.println("No OnClick");
//                    noButton.setSelected(true);
//                    yesButton.setSelected(false);
//                }
//            });
//
//            ll.setOrientation(LinearLayout.HORIZONTAL);
//            ll.setWeightSum(2);
//            ll.addView(yesButton);
//            ll.addView(noButton);
//
//            alertBuilder.setView(ll);
//
//        }  // end Value if/else statement
//
//        alertBuilder.setNeutralButton("Update", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                System.out.println("Update Button Clicked");
//                // Update the value
//                if (!isToggleable) {
//                    // Grab the new value
//                    updatedData = valueText.getText().toString();
//                } else {
//                    // Update the value if the button has been clicked
//                    if (yesButton.isSelected()) {
//                        updatedData = "1";
//                    } else if (noButton.isSelected()) {
//                        updatedData = "0";
//                    } else {
//                        updatedData = "";
//                    }
//                }  // end onCLick value if/else
//
//                // Update the data
//                onDataChanged(title, updatedData);
//                dialog.dismiss();
//            }
//        });
//
//        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                updatedData = "";
//                dialog.dismiss();
//            }
//        });
//
//        return alertBuilder.create();
//
//    }
//
//    public void setOnDataChangedListener(DataListener dr) {
//        this.dr = dr;
//    }
//
//    public void onDataChanged(String name, String data) {
//        dr.onDataChanged(name, data);
//    }
//}
