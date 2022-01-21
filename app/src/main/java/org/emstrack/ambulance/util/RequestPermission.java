package org.emstrack.ambulance.util;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import org.emstrack.ambulance.BuildConfig;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.dialogs.AlertSnackbar;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.util.BroadcastActions;
import org.emstrack.models.util.OnServiceComplete;

import java.util.Arrays;

public class RequestPermission {

    private static final String TAG = RequestPermission.class.getSimpleName();

    private final ActivityResultLauncher<String[]> activityResultLauncher;
    private final Fragment fragment;
    private String[] permissions;
    private boolean promptIfNotGranted;

    private OnPermissionGranted onPermissionGranted;

    public interface OnPermissionGranted {
        void permissionGranted(boolean granted);
    }

    public RequestPermission(Fragment fragment) {

        // save fragment
        this.fragment = fragment;

        // Build permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.i(TAG, "Permissions version >= R");
            if (RequestPermissionHelper.checkPermissions(fragment.requireContext(), new String[] {Manifest.permission.ACCESS_COARSE_LOCATION})
                    || RequestPermissionHelper.checkPermissions(fragment.requireContext(), new String[] {Manifest.permission.ACCESS_FINE_LOCATION})) {
                // has coarse or fine location, ask for all
                Log.i(TAG, "Will ask for BACKGROUND LOCATION first");
                this.permissions = new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                };
            } else {
                Log.i(TAG, "Will ask for FOREGROUND LOCATION");
                // does not have foreground location, start with foreground first
                this.permissions = new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                };
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.i(TAG, "Permissions version >= Q");
            this.permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            };
        } else {
            Log.i(TAG, "Permissions version < Q");
            this.permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }

        // register launcher
        activityResultLauncher =
                fragment.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                        isGrantedMap -> {

                            Log.i(TAG, "Permissions results:");
                            Log.i(TAG, isGrantedMap.toString());

                            // check all permissions
                            boolean granted = true;
                            for (String permission: this.permissions) {
                                //noinspection ConstantConditions
                                if (isGrantedMap.containsKey(permission) && isGrantedMap.get(permission)) {
                                    continue;
                                }
                                granted = false;
                                break;
                            }
                            this.action(granted);
                        }
                );

        this.promptIfNotGranted = true;

        // set default onPermissionGranted
        this.onPermissionGranted = granted -> {
            if (granted) {
                Log.d(TAG, "Permission granted");
            } else {
                Log.d(TAG, "Permission not granted");
            }
        };

    }

    public void setOnPermissionGranted(OnPermissionGranted onPermissionGranted) {
        this.onPermissionGranted = onPermissionGranted;
    }

    private void action(boolean granted) {
        if (granted) {
            Log.i(TAG, "Permissions granted");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Arrays.asList(this.permissions).contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                Log.i(TAG, "Permissions version >= R, need to ask for BACKGROUND LOCATION");
                this.permissions = new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                };

                // create permission helper
                RequestPermissionHelper requestPermissionHelper = new RequestPermissionHelper(
                        fragment.requireContext(), fragment.requireActivity(), this.permissions);

                // fire request, permissions will be denied and processed by user
                this.promptIfNotGranted = false;
                requestPermissionHelper.checkAndRequest(activityResultLauncher,
                        fragment.getString(R.string.locationPermissionMessageVersionRMessage2)
                                + "\n\n" +
                                fragment.getString(R.string.locationPermissionSettingsMessage,
                                        fragment.getString(android.R.string.ok),
                                        fragment.getString(R.string.versionRPermissionOption,
                                                fragment.requireContext().getPackageManager().getBackgroundPermissionOptionLabel()))
                                + "\n\n" +
                                fragment.getString(R.string.locationPermissionMessage)
                );

            } else {
                Log.i(TAG, "Check settings");
                checkLocationSettings();
            }

        } else if (this.promptIfNotGranted) {
            Log.i(TAG, "Permissions have not been granted, will launch prompt.");
            // Notify the user via a SnackBar that they have rejected a core permission for the
            // app, which makes the Activity useless.

            // Additionally, it is important to remember that a permission might have been
            // rejected without asking the user for permission (device policy or "Never ask
            // again" prompts). Therefore, a user interface affordance is typically implemented
            // when permissions are denied. Otherwise, your app could appear unresponsive to
            // touches or interactions which have required permissions.
            final String message = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R ?
                    fragment.getString(R.string.versionRPermissionOption,
                            fragment.requireContext().getPackageManager().getBackgroundPermissionOptionLabel()) :
                    "";

            // dismiss first, then go to settings
            // Build intent that displays the App settings screen.
            // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            new AlertDialog.Builder(fragment.getActivity())
                    .setTitle(R.string.needPermissions)
                    .setMessage(fragment.getString(R.string.locationPermissionMessage) +
                            "\n\n" +
                            fragment.getString(R.string.locationPermissionSettingsMessage, fragment.getString(android.R.string.ok), message))
                    .setPositiveButton(android.R.string.ok,
                            (dialog, which) -> {

                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                fragment.startActivity(intent);

                            })
                    .setNegativeButton(android.R.string.cancel,
                            (dialog, which) -> new AlertSnackbar(fragment.requireActivity())
                                    .alert(fragment.getString(R.string.expectLimitedFunctionality)))
                    .setCancelable(false)
                    .create()
                    .show();

        }
    }

    private void checkLocationSettings() {
        // check location settings
        Intent intent = new Intent(fragment.requireActivity(), AmbulanceForegroundService.class);
        intent.setAction(AmbulanceForegroundService.Actions.CHECK_LOCATION_SETTINGS);

        new OnServiceComplete(fragment.requireActivity(),
                BroadcastActions.SUCCESS,
                BroadcastActions.FAILURE,
                intent) {

            @Override
            public void onSuccess(Bundle extras) {
                onPermissionGranted.permissionGranted(true);
            }

        }
                .setFailureMessage(fragment.getString(R.string.expectLimitedFunctionality))
                .setAlert(new AlertSnackbar(fragment.requireActivity()))
                .start();
    }

    public void check() {
        Log.i(TAG, "Checking permissions");

        if (AmbulanceForegroundService.canUpdateLocation()) {
            Log.i(TAG, "Location settings already satisfied");
            onPermissionGranted.permissionGranted(true);
        } else {
            Log.i(TAG, "Location settings not satisfied, checking for permission");

            String message = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ?
                    fragment.getString(R.string.locationPermissionMessage) + "\n\n" + fragment.getString(R.string.locationPermissionMessageVersionRMessage1) :
                    fragment.getString(R.string.locationPermissionMessage);

            // create permission helper
            RequestPermissionHelper requestPermissionHelper = new RequestPermissionHelper(fragment.requireContext(),
                    fragment.requireActivity(), this.permissions);

            // fire request
            this.promptIfNotGranted = true;
            if ( requestPermissionHelper.checkAndRequest(activityResultLauncher, message) ) {
                Log.i(TAG, "Permissions granted but not checked; checking location settings");
                checkLocationSettings();
            } else {
                Log.i(TAG, "Permissions denied, interacting with user.");
            }
        }
    }

}
