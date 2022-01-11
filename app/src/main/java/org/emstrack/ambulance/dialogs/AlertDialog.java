package org.emstrack.ambulance.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.util.Log;

import org.emstrack.models.util.Alert;

/**
 * Alert dialog
 *
 * @author mauricio
 * @since 02/10/2019
 */

public class AlertDialog extends Alert {

    private static String TAG = AlertDialog.class.getSimpleName();

    private final android.app.AlertDialog.Builder builder;
    private final DialogInterface.OnClickListener onClickListener;
    private android.app.AlertDialog alert;

    public AlertDialog(String TAG) {
        this.TAG = TAG;
        this.builder = null;
        this.onClickListener = null;
        this.alert = null;
    }

    public AlertDialog(Activity activity, String title,
                       DialogInterface.OnClickListener onClickListener) {

        // create builder
        this.builder = new android.app.AlertDialog.Builder(activity);

        this.builder.setTitle(title);
        this.builder.setCancelable(false);
        this.onClickListener = onClickListener;

    }

    public AlertDialog(Activity activity, String title) {
        this(activity, title, (dialog, which) -> { /* do nothing */ });
    }

    public void setTag(String TAG) {
        this.TAG = TAG;
    }

    public void alert(String message) {
        alert(message, this.onClickListener);
    }

    public void alert(String message,
                      DialogInterface.OnClickListener onOkClickAction) {

        // Log message
        Log.d(TAG, message);

        if (this.builder != null) {

            // set message
            this.builder.setMessage(message);

            // Build dialog
            alert = this.builder.setPositiveButton(android.R.string.ok, onOkClickAction)
                    .create();

            alert.show();
        }

    }

    public void dismiss() {
        if (this.alert != null) {
            this.alert.dismiss();
        }
    }

}
