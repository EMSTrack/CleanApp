package org.emstrack.hospital.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.emstrack.hospital.HospitalApp;
import org.emstrack.hospital.LoginActivity;
import org.emstrack.hospital.R;
import org.emstrack.mqtt.MqttProfileClient;

/**
 * Created by devinhickey on 5/24/17.
 */

public class LogoutDialog extends DialogFragment {

    final String TAG = "LogoutDialog";

    public LogoutDialog() {}

    public static LogoutDialog newInstance() {
        return new LogoutDialog();
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        System.out.println("Logout Dialog onCreateDialog");

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());

        alertBuilder.setTitle(getResources().getString(R.string.alert_warning));
        alertBuilder.setMessage(getResources().getString(R.string.logout_confirm));

        // Create the OK button that logs user out
        alertBuilder.setNeutralButton(getResources().getString(R.string.alert_button_positive_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.out.println("OK Button Clicked");

                // Retrieve client
                final MqttProfileClient profileClient = ((HospitalApp) getActivity().getApplication()).getProfileClient();
                try {
                    profileClient.disconnect();
                } catch (MqttException e) {
                    Log.d(TAG,"Failed to disconnect.");
                }

                Intent rootIntent = new Intent(getActivity(), LoginActivity.class);
                rootIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(rootIntent);

            }
        });

        // Create the Cancel Button
        alertBuilder.setNegativeButton(getResources().getString(R.string.alert_button_negative_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.out.println("Cancel Button Clicked");
                dialog.dismiss();
            }
        });

        return alertBuilder.create();

    }

}
