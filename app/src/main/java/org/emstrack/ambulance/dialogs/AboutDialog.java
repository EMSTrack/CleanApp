package org.emstrack.ambulance.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

import org.emstrack.ambulance.LoginActivity;
import org.emstrack.ambulance.R;

/**
 * Created by mauricio on 4/26/18.
 */

public class AboutDialog {

    private static final String TAG = AboutDialog.class.getSimpleName();

    public static AlertDialog newInstance(final Activity activity) {

        // Inflate the about message contents
        View messageView = activity.getLayoutInflater().inflate(R.layout.about, null, false);

        // Logout dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setIcon(R.drawable.ic_ambulance);
        builder.setTitle(R.string.app_name);
        builder.setView(messageView);
        return builder.create();

    }

}
