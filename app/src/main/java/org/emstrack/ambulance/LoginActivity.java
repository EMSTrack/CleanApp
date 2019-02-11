package org.emstrack.ambulance;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;

import org.emstrack.ambulance.dialogs.AlertSnackbar;
import org.emstrack.ambulance.dialogs.VersionDialog;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.Credentials;
import org.emstrack.models.Profile;
import org.emstrack.models.util.BroadcastActions;
import org.emstrack.models.util.BroadcastExtras;
import org.emstrack.models.util.OnServiceComplete;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = LoginActivity.class.getSimpleName();

    public static final String LOGOUT = "org.emstrack.ambulance.LoginActivity.LOGOUT";

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    private SharedPreferences sharedPreferences;
    private Button loginSubmitButton;
    private TextView usernameField;
    private TextView passwordField;
    private Spinner serverField;
    private boolean logout;

    private ArrayAdapter<CharSequence> serverNames;
    private String[] serverList;
    private List<String> serverMqttURIs;
    private List<String> serverAPIURIs;

    private BroadcastReceiver versionUpdateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d(TAG, "onCreate");

        // retrieveObject action
        String action = getIntent().getAction();
        logout = LOGOUT.equals(action);

        // Find username and password from layout
        usernameField = findViewById(R.id.editUserName);
        passwordField = findViewById(R.id.editPassword);

        // Retrieve list of servers
        serverList = getResources().getStringArray(R.array.spinner_list_item_array_server);

        // Populate server list
        // Log.d(TAG, "Populating server list");
        serverNames = new ArrayAdapter(this, android.R.layout.simple_spinner_item);
        serverMqttURIs = new ArrayList<>();
        serverAPIURIs = new ArrayList<>();
        for (String server: serverList) {
            try {
                String[] splits = server.split(":", 3);
                serverNames.add(splits[0]);
                if (!splits[1].isEmpty()) {
                    serverMqttURIs.add("ssl://" + splits[1] + ":" + splits[2]);
                    serverAPIURIs.add("https://" + splits[1]);
                } else {
                    serverMqttURIs.add("");
                    serverAPIURIs.add("");
                }
            } catch (Exception e) {
                Log.d(TAG, "Malformed server string. Skipping.");
            }
        }
        // Log.d(TAG, serverMqttURIs.toString());

        // Create server spinner
        serverField = findViewById(R.id.spinnerServer);
        serverNames.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serverField.setAdapter(serverNames);

        // Retrieving credentials
        sharedPreferences = getSharedPreferences(AmbulanceForegroundService.PREFERENCES_NAME, MODE_PRIVATE);

        // Retrieve past credentials
        usernameField.setText(sharedPreferences.getString(AmbulanceForegroundService.PREFERENCES_USERNAME, null));
        passwordField.setText(sharedPreferences.getString(AmbulanceForegroundService.PREFERENCES_PASSWORD, null));
        String serverMqttUri = sharedPreferences.getString(AmbulanceForegroundService.PREFERENCES_MQTT_SERVER, null);

        // set server item
        int serverPos = 0;
        if (serverMqttUri != null) {
            serverPos = serverMqttURIs.indexOf(serverMqttUri);
        }
        if (serverPos < 0)
            serverPos = 0;
        serverField.setSelection(serverPos);

        // Submit button
        loginSubmitButton = findViewById(R.id.buttonLogin);

        // allow keyboard to disappear on screen click
        findViewById(R.id.relativeLayout).setOnTouchListener(
                (v, event) -> {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    return true;
                });

        if (versionUpdateReceiver == null) {
            versionUpdateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    createUpdateAppDialog();
                    Log.d(TAG, "App version is not up to date!");
                }
            };
            getLocalBroadcastManager().registerReceiver(versionUpdateReceiver,
                    new IntentFilter(AmbulanceForegroundService.BroadcastActions.VERSION_UPDATE));
        }

    }

    public void disableLogin() {

        // Disable login
        loginSubmitButton.setOnClickListener(
                v -> {

                    // Toast to warn about check permissions
                    Toast.makeText(LoginActivity.this,
                            R.string.checkingResources,
                            Toast.LENGTH_LONG).show();

                });

    }

    private void logoutFirst(String uuid) {

        if (!this.logout) {

            Log.d(TAG,"No need to logout.");

            // broadcast success
            Intent localIntent = new Intent(org.emstrack.models.util.BroadcastActions.SUCCESS);
            localIntent.putExtra(BroadcastExtras.UUID, uuid);
            getLocalBroadcastManager().sendBroadcast(localIntent);

            // then return
            return;

        }

        Log.d(TAG,"Logout first.");

        // Create stop foreground service intent
        Intent stopIntent = new Intent(this, AmbulanceForegroundService.class);
        stopIntent.setAction(AmbulanceForegroundService.Actions.STOP_SERVICE);

        // Chain services
        new OnServiceComplete(this,
                BroadcastActions.SUCCESS,
                BroadcastActions.FAILURE,
                stopIntent) {

            @Override
            public void onSuccess(Bundle extras) {

                Log.i(TAG, "onSuccess");

                // Set logout to false
                logout = false;

                // broadcast success
                Intent localIntent = new Intent(BroadcastActions.SUCCESS);
                localIntent.putExtra(BroadcastExtras.UUID, uuid);
                getLocalBroadcastManager().sendBroadcast(localIntent);

            }

            @Override
            public void onFailure(Bundle extras) {
                super.onFailure(extras);

                // broadcast failure
                Intent localIntent = new Intent(BroadcastActions.FAILURE);
                localIntent.putExtra(BroadcastExtras.UUID, uuid);
                getLocalBroadcastManager().sendBroadcast(localIntent);

            }
        }
                .setFailureMessage(this.getString(R.string.couldNotLogout))
                .setAlert(new AlertSnackbar(this))
                .start();
    }

    public void enableLogin() {

        Log.d(TAG, "enableLogin");

        // Enable login button
        loginSubmitButton.setOnClickListener(this);

        // Create start foreground activity intent
        Intent startIntent = new Intent(LoginActivity.this,
                AmbulanceForegroundService.class);
        startIntent.putExtra("ADD_STOP_ACTION", true);
        startIntent.setAction(AmbulanceForegroundService.Actions.START_SERVICE);

        // Logout then login or start service
        new OnServiceComplete(this,
                BroadcastActions.SUCCESS,
                BroadcastActions.FAILURE,
                null) {

            @Override
            public void run() {

                // logout first?
                logoutFirst(getUuid());

            }

            @Override
            public void onSuccess(Bundle extras) {

                // Already logged in?
                AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                if (appData != null && appData.getProfile() != null) {

                    Log.i(TAG, "Already logged in, starting MainActivity.");

                    // Get username
                    Credentials credentials = appData.getCredentials();
                    if (credentials != null) {

                        // final String username = usernameField.getText().toString().trim();
                        final String username = credentials.getUsername();

                        // Create intent
                        Intent intent = new Intent(LoginActivity.this,
                                MainActivity.class);

                        // Toast
                        Toast.makeText(LoginActivity.this,
                                getResources().getString(R.string.loginSuccessMessage, username),
                                Toast.LENGTH_SHORT).show();

                        // initiate MainActivity
                        startActivity(intent);

                    } else {

                        // Toast
                        Toast.makeText(LoginActivity.this,
                                getResources().getString(R.string.couldNotLogin),
                                Toast.LENGTH_SHORT).show();

                    }

                } else{

                    Log.i(TAG, "Could not find profile, starting service");

                    // Initialize service to make sure it gets bound to service
                    Intent intent = new Intent(LoginActivity.this,
                            AmbulanceForegroundService.class);
                    intent.putExtra("ADD_STOP_ACTION", true);
                    intent.setAction(AmbulanceForegroundService.Actions.START_SERVICE);

                    new OnServiceComplete(LoginActivity.this,
                            BroadcastActions.SUCCESS,
                            BroadcastActions.FAILURE,
                            intent) {

                        @Override
                        public void onSuccess(Bundle extras) {

                            if (AmbulanceForegroundService.canUpdateLocation()) {

                                // Toast to warn about check permissions
                                Toast.makeText(LoginActivity.this,
                                        R.string.permissionsSatisfied,
                                        Toast.LENGTH_SHORT).show();

                            } else {

                                // Alert to warn about check permissions
                                new AlertSnackbar(LoginActivity.this)
                                        .alert(getString(R.string.expectLimitedFuncionality));

                            }

                        }
                    }
                            .setFailureMessage(LoginActivity.this.getString(R.string.couldNotStartService))
                            .setAlert(new AlertSnackbar(LoginActivity.this))
                            .start();

                }

            }
        }
                .setFailureMessage(this.getString(R.string.couldNotLogout))
                .setAlert(new AlertSnackbar(this))
                .start();

    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");

        // Disable login
        disableLogin();

        if (versionUpdateReceiver == null) {
            versionUpdateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    createUpdateAppDialog();
                    Log.d(TAG, "App version is not up to date!");
                }
            };
            getLocalBroadcastManager().registerReceiver(versionUpdateReceiver,
                    new IntentFilter(AmbulanceForegroundService.BroadcastActions.VERSION_UPDATE));
        }

        // Can updateAmbulance?
        if (AmbulanceForegroundService.canUpdateLocation()) {

            // Enable login
            enableLogin();

            // Check and request permissions to retrieve locations if necessary
        } else if (checkPermissions()) {

            // Otherwise check
            checkLocationSettings();

        } else {

            // requestPermissions call_current checkLocationSettings if successful
            requestPermissions();

        }

    }

    @Override
    public void onClick(View view) {

        // Get user info & remove whitespace
        final String username = usernameField.getText().toString().trim();
        final String password = passwordField.getText().toString().trim();

        final String serverUri = serverMqttURIs.get(serverField.getSelectedItemPosition());
        final String serverApiUri = serverAPIURIs.get(serverField.getSelectedItemPosition());
        Log.d(TAG, "Logging into server: " + serverUri);

        /*String serverName = serverField.getSelectedItem().toString();
        int serverPos = serverList.getPosition(serverName);
        final String server = serverURLs.getItem(serverPos).toString();*/

        if (username.isEmpty())
            new AlertSnackbar(LoginActivity.this).alert(getResources().getString(R.string.error_empty_username));

        else if (password.isEmpty())
            new AlertSnackbar(LoginActivity.this).alert(getResources().getString(R.string.error_empty_password));

        else if (serverUri.isEmpty())
            new AlertSnackbar(LoginActivity.this).alert(getResources().getString(R.string.error_invalid_server));

        else if (serverApiUri.isEmpty())
            new AlertSnackbar(LoginActivity.this).alert(getResources().getString(R.string.error_invalid_server));

        else {

            Log.d(TAG, "Will offer credentials");

            // Login at service
            Intent intent = new Intent(LoginActivity.this,
                    AmbulanceForegroundService.class);
            intent.setAction(AmbulanceForegroundService.Actions.LOGIN);
            intent.putExtra(AmbulanceForegroundService.BroadcastExtras.CREDENTIALS,
                    new String[]{username, password, serverUri, serverApiUri});

            // What to do when service completes?
            new OnServiceComplete(LoginActivity.this,
                    BroadcastActions.SUCCESS,
                    BroadcastActions.FAILURE,
                    intent) {

                @Override
                public void onSuccess(Bundle extras) {
                    Log.i(TAG, "onClick:OnServiceComplete:onSuccess");

                    // Toast
                    Toast.makeText(LoginActivity.this,
                            getResources().getString(R.string.loginSuccessMessage, username),
                            Toast.LENGTH_SHORT).show();

                    // initiate MainActivity
                    Intent intent = new Intent(LoginActivity.this,
                            MainActivity.class);
                    startActivity(intent);

                }

            }
                    .setFailureMessage(getResources().getString(R.string.couldNotLoginUser, username))
                    .setAlert(new AlertSnackbar(LoginActivity.this))
                    .start();

        }

    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request permission to access fine location
     */
    private void requestPermissions() {

        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");

            new AlertSnackbar(this)
                    .alert(getString(R.string.permission_rationale),
                            view -> {
                                // Request permission
                                ActivityCompat.requestPermissions(LoginActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        REQUEST_PERMISSIONS_REQUEST_CODE);
                            });

        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(LoginActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * APICallback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {

                // If user interaction was interrupted,
                // the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");

            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Permission granted
                Log.i(TAG, "Permission granted");

                // Will check location settings
                checkLocationSettings();

            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                new AlertSnackbar(this)
                        .alert(getString(R.string.permission_denied_explanation),
                                view -> {
                                    // Build intent that displays the App settings screen.
                                    Intent intent = new Intent();
                                    intent.setAction(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package",
                                            BuildConfig.APPLICATION_ID, null);
                                    intent.setData(uri);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                });
            }
        }
    }

    public void checkLocationSettings() {

        // Build settings client
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);

        // Check if the device has the necessary location settings.
        settingsClient.checkLocationSettings(AmbulanceForegroundService.getLocationSettingsRequest())
                .addOnSuccessListener(this,
                        locationSettingsResponse -> {
                            Log.i(TAG, "All location settings are satisfied.");

                            // enable location updates
                            AmbulanceForegroundService.setCanUpdateLocation(true);

                            // enable login
                            enableLogin();

                        })
                .addOnFailureListener(this,
                        e -> {
                            int statusCode = ((ApiException) e).getStatusCode();
                            switch (statusCode) {
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                            "location settings ");
                                    try {
                                        // Show the dialog by calling startResolutionForResult(), and check the
                                        // result in onActivityResult().
                                        ResolvableApiException rae = (ResolvableApiException) e;
                                        rae.startResolutionForResult(LoginActivity.this, REQUEST_CHECK_SETTINGS);
                                    } catch (IntentSender.SendIntentException sie) {
                                        Log.i(TAG, "PendingIntent unable to execute request.");
                                    }
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    new AlertSnackbar(LoginActivity.this)
                                            .alert(getString(R.string.settingsAreInadequate));

                                    // disable location updates
                                    AmbulanceForegroundService.setCanUpdateLocation(false);

                                    // enable login
                                    enableLogin();

                            }

                        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        break;
                }
                break;
        }
    }

    /**
     * Get the LocalBroadcastManager
     *
     * @return The system LocalBroadcastManager
     */
    private LocalBroadcastManager getLocalBroadcastManager() {
        return LocalBroadcastManager.getInstance(this);
    }

    private void createUpdateAppDialog() {
        Log.d(TAG, "Generating dialog to prompt user to update app");
        VersionDialog.newInstance(this).show();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        try {
            getLocalBroadcastManager().unregisterReceiver(versionUpdateReceiver);
        } catch (Throwable t) {
            Log.d(TAG, "Version update receiver not registered", t);
        }
    }
}
