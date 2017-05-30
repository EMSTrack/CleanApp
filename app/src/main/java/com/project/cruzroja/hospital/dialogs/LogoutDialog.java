package com.project.cruzroja.hospital.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

/**
 * Created by devinhickey on 5/24/17.
 */

public class LogoutDialog extends DialogFragment {


    public LogoutDialog() {}


    public static LogoutDialog newInstance() {
        LogoutDialog ld = new LogoutDialog();

        return ld;
    }


    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        System.out.println("Logout Dialog onCreateDialog");

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());

        alertBuilder.setTitle("Warning");
        alertBuilder.setMessage("Are you sure you want to logout?");

        // Create the OK button that logs user out
        alertBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.out.println("OK Button Clicked");
                getActivity().finish();
            }
        });

        // Create the Cancel Button
        alertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.out.println("Cancel Button Clicked");
                dialog.dismiss();
            }
        });

        return alertBuilder.create();

    }

}
