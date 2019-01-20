package org.emstrack.ambulance.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.emstrack.ambulance.BuildConfig;
import org.emstrack.ambulance.LoginActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.Ambulance;

import java.util.Date;

/**
 * Created by mauricio on 4/26/18.
 */

public class AboutDialog {

    private static final String TAG = AboutDialog.class.getSimpleName();

    public static AlertDialog newInstance(final Activity activity) {

        // Inflate the about message contents
        View messageView = activity.getLayoutInflater().inflate(R.layout.about, null, false);

        // Set build date
        TextView buildDate = messageView.findViewById(R.id.buildDate);
        buildDate.setText(new Date(BuildConfig.TIMESTAMP).toString());

        // Set build version
        TextView buildVersion = messageView.findViewById(R.id.buildVersion);
        buildVersion.setText(R.string.app_version);

        // Logout dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setIcon(R.drawable.ambulancelogo);
        builder.setTitle(R.string.app_name);
        builder.setView(messageView);

        // Create OK button
        builder.setPositiveButton(R.string.ok, null);

        return builder.create();

    }

}
