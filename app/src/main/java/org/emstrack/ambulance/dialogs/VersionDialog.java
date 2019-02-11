package org.emstrack.ambulance.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;

import org.emstrack.ambulance.R;

public class VersionDialog {
    private static final String TAG = VersionDialog.class.getSimpleName();

    public static AlertDialog newInstance(final Activity activity) {

        // Version Update dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);

        alertDialogBuilder.setTitle(R.string.alert_warning_title);
        alertDialogBuilder.setMessage(R.string.version_update);

        // Create the OK button that logs user out
        alertDialogBuilder.setPositiveButton(
                R.string.ok,
                (dialog, which) -> {

                    Log.i(TAG,"VersionDialog: OK Button Clicked");


                });

        return alertDialogBuilder.create();

    }
}
