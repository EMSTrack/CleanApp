package org.emstrack.ambulance.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.emstrack.ambulance.LoginActivity;
import org.emstrack.ambulance.R;

/**
 * Created by devinhickey on 5/24/17.
 */

public class LogoutDialog {

    private static final String TAG = LogoutDialog.class.getSimpleName();

    public static AlertDialog newInstance(final Activity activity) {

        // Logout dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);

        alertDialogBuilder.setTitle(R.string.alert_warning_title);
        alertDialogBuilder.setMessage(R.string.logout_confirm);

        // Cancel button
        alertDialogBuilder.setNegativeButton(
                R.string.alert_button_negative_text,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        /* do nothing */
                    }
                });

        // Create the OK button that logs user out
        alertDialogBuilder.setPositiveButton(
                R.string.alert_button_positive_text,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Log.i(TAG,"LogoutDialog: OK Button Clicked");

                        // Start logout activity
                        Intent loginIntent = new Intent(activity, LoginActivity.class);
                        loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        loginIntent.setAction(LoginActivity.LOGOUT);
                        activity.startActivity(loginIntent);

                    }
                });

        return alertDialogBuilder.create();

    }

}
