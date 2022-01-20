package org.emstrack.ambulance.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.emstrack.ambulance.R;
import org.emstrack.ambulance.dialogs.AlertSnackbar;

public class RequestPermissionHelper {

    private static final String TAG = "RequestPermission";

    private final Context context;
    private final String[] permissions;
    private final Activity activity;

    public RequestPermissionHelper(Context context, Activity activity, String[] permissions) {
        this.context = context;
        this.activity = activity;
        this.permissions = permissions;
    }

    public static boolean checkPermissions(Context context, String[] permissions) {
        for (String permission: permissions) {
            if ( !(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) )
                return false;
        }
        return true;
    }

    public boolean checkPermissions() {
        for (String permission: this.permissions) {
            if ( !(ContextCompat.checkSelfPermission(this.context, permission) == PackageManager.PERMISSION_GRANTED) )
                return false;
        }
        return true;
    }

    public boolean shouldShowRequestPermissionRationale() {
        for (String permission: this.permissions) {
            if ( ActivityCompat.shouldShowRequestPermissionRationale(this.activity, permission) )
                return true;
        }
        return false;
    }

    public boolean checkAndRequest(ActivityResultLauncher<String[]> launcher, String rationale) {

        if (this.checkPermissions()) {
            Log.i(TAG, "Permissions granted.");
            // You can use the API that requires the permission.
            return true;

        } else if (this.shouldShowRequestPermissionRationale()) {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            Log.i(TAG, "Displaying permission rationale to provide additional context.");

            new AlertDialog.Builder(this.activity)
                    .setTitle(R.string.needPermissions)
                    .setMessage(rationale)
                    .setPositiveButton(android.R.string.ok,
                            (dialog, which) -> launcher.launch(this.permissions)
                    )
                    .setNegativeButton(android.R.string.cancel, (dialog, which) ->
                            new AlertSnackbar(this.activity)
                                    .alert(this.context.getString(R.string.expectLimitedFunctionality)))
                    .setCancelable(false)
                    .create()
                    .show();
            return false;

        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            Log.i(TAG, "Requesting permissions.");

            launcher.launch(this.permissions);
            return false;
        }

    }

}
