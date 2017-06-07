package com.project.cruzroja.hospital.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import com.project.cruzroja.hospital.LoginActivity;
import com.project.cruzroja.hospital.MqttClient;

import static android.content.Context.MODE_PRIVATE;

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

        alertBuilder.setTitle("¡Advertencia!");
        alertBuilder.setMessage("¿Seguro que desea cerrar sesión?");

        // Create the OK button that logs user out
        alertBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.out.println("OK Button Clicked");
                MqttClient client = MqttClient.getInstance(getActivity().getApplicationContext());
                client.disconnect();

                /*SharedPreferences creds_prefs = getActivity().getSharedPreferences("com.project.cruzroja.hospital", MODE_PRIVATE);
                SharedPreferences.Editor editor = creds_prefs.edit();
                editor.clear();
                editor.commit(); */

                Intent rootIntent = new Intent(getActivity(), LoginActivity.class);
                rootIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(rootIntent);

            }
        });

        // Create the Cancel Button
        alertBuilder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                System.out.println("Cancel Button Clicked");
                dialog.dismiss();
            }
        });

        return alertBuilder.create();

    }

}
