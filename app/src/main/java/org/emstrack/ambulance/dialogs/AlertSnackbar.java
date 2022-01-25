package org.emstrack.ambulance.dialogs;

import android.app.Activity;
import com.google.android.material.snackbar.Snackbar;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import org.emstrack.models.util.Alert;

/**
 * Created by mauricio on 3/22/2018.
 */

public class AlertSnackbar extends Alert {

    private String TAG = AlertSnackbar.class.getSimpleName();
    private final View view;

    public AlertSnackbar(@NonNull String TAG) {
        this.TAG = TAG;
        this.view = null;
    }

    public AlertSnackbar(@NonNull View view) {
        this.view = view.findViewById(android.R.id.content);
    }

    public AlertSnackbar(@NonNull Activity activity) {
        this.view = activity.findViewById(android.R.id.content);
    }

    public void setTag(@NonNull String TAG) {
        this.TAG = TAG;
    }

    public void alert(@NonNull String message) {
        alert(message, view -> { /* do nothing */ });
    }

    public void alert(@NonNull String message,
                      @NonNull View.OnClickListener onOkClickAction) {

        // Log message
        Log.d(TAG, message);

        // Display alert as snack bar
        if (view != null)
            Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, onOkClickAction)
                    .show();


    }

}
