package org.emstrack.ambulance.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.emstrack.ambulance.AmbulanceApp;
import org.emstrack.ambulance.AmbulanceListActivity;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.LoginActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.services.OnServiceComplete;
import org.emstrack.mqtt.MqttProfileClient;

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

                        // Stop foreground activity
                        Intent intent = new Intent(activity, AmbulanceForegroundService.class);
                        intent.setAction(AmbulanceForegroundService.Actions.STOP_SERVICE);

                        // What to do when service completes?
                        new OnServiceComplete(activity,
                                AmbulanceForegroundService.BroadcastActions.SUCCESS,
                                AmbulanceForegroundService.BroadcastActions.FAILURE,
                                intent) {

                            @Override
                            public void onSuccess(Bundle extras) {
                                Log.i(TAG, "onSuccess");

                                // Start login activity
                                Intent loginIntent = new Intent(activity, LoginActivity.class);
                                loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                activity.startActivity(loginIntent);

                            }

                        }
                                .setFailureMessage(activity.getString(R.string.couldNotLogout))
                                .setAlert(new AlertSnackbar(activity));

                    }
                });

        return alertDialogBuilder.create();

    }

}
