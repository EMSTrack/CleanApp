package org.emstrack.ambulance;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
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
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.emstrack.ambulance.dialogs.AlertSnackbar;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.ambulance.services.OnServiceComplete;
import org.emstrack.models.Ambulance;
import org.emstrack.models.AmbulancePermission;
import org.emstrack.models.Profile;
import org.emstrack.mqtt.MqttProfileCallback;
import org.emstrack.mqtt.MqttProfileClient;

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

    ArrayAdapter<CharSequence> serverNames;
    String[] serverList;
    List<String> serverURIs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d(TAG, "onCreate");

        // get action
        String action = getIntent().getAction();
        logout = LOGOUT.equals(action);

        // Find username and password from layout
        usernameField = (TextView) findViewById(R.id.editUserName);
        passwordField = (TextView) findViewById(R.id.editPassword);

        // Retrieve list of servers
        serverList = getResources().getStringArray(R.array.spinner_list_item_array_server);

        // Populate server list
        Log.d(TAG, "Populating server list");
        serverNames = new ArrayAdapter(this, android.R.layout.simple_spinner_item);
        serverURIs = new ArrayList<>();
        for (String server: serverList) {
            try {
                String[] splits = server.split(":", 3);
                serverNames.add(splits[0]);
                if (!splits[1].isEmpty()) {
                    serverURIs.add("ssl://" + splits[1] + ":" + splits[2]);
                } else {
                    serverURIs.add("");
                }
            } catch (Exception e) {
                Log.d(TAG, "Malformed server string. Skipping.");
            }
        }
        Log.d(TAG, serverURIs.toString());

        // Create server spinner
        serverField = (Spinner) findViewById(R.id.spinnerServer);
        serverNames.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serverField.setAdapter(serverNames);

/*
        serverURLs = ArrayAdapter.createFromResource(this,
                R.array.list_item_array_server_url, android.R.layout.simple_spinner_item);
*/

        // Retrieving credentials
        sharedPreferences = getSharedPreferences(AmbulanceForegroundService.PREFERENCES_NAME, MODE_PRIVATE);

        // Retrieve past credentials
        usernameField.setText(sharedPreferences.getString(AmbulanceForegroundService.PREFERENCES_USERNAME, null));
        passwordField.setText(sharedPreferences.getString(AmbulanceForegroundService.PREFERENCES_PASSWORD, null));
        String serverUri = sharedPreferences.getString(AmbulanceForegroundService.PREFERENCES_SERVER, null);

        // set server item
        int serverPos = 0;
        if (serverUri != null) {
            serverPos = serverURIs.indexOf(serverUri);
        }
        if (serverPos < 0)
            serverPos = 0;
        serverField.setSelection(serverPos);

        // Submit button
        loginSubmitButton = (Button) findViewById(R.id.buttonLogin);

        // allow keyboard to disappear on screen click
        findViewById(R.id.relativeLayout).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                return true;
            }
        });

    }

    public void disableLogin() {

        // Disable login
        loginSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Toast to warn about check permissions
                Toast.makeText(LoginActivity.this, "Please be patient. Checking permissions...", Toast.LENGTH_LONG).show();

            }
        });

    }

    public void enableLogin() {

        Log.d(TAG, "enableLogin");

        // Enable login
        loginSubmitButton.setOnClickListener(this);

        // Logout first?
        if (logout) {

            Log.d(TAG,"Logout first.");

            // Stop foreground activity
            Intent intent = new Intent(this, AmbulanceForegroundService.class);
            intent.setAction(AmbulanceForegroundService.Actions.STOP_SERVICE);

            // What to do when service completes?
            new OnServiceComplete(this,
                    AmbulanceForegroundService.BroadcastActions.SUCCESS,
                    AmbulanceForegroundService.BroadcastActions.FAILURE,
                    intent) {

                @Override
                public void onSuccess(Bundle extras) {
                    Log.i(TAG, "onSuccess");

                    // Set logout to false
                    logout = false;

                    // Initialize service to make sure it gets bound to service
                    Intent intent = new Intent(LoginActivity.this, AmbulanceForegroundService.class);
                    intent.putExtra("ADD_STOP_ACTION", true);
                    intent.setAction(AmbulanceForegroundService.Actions.START_SERVICE);
                    startService(intent);

                    // TODO: is this safe to do asynchronously?

                }

            }
                    .setFailureMessage(this.getString(R.string.couldNotLogout))
                    .setAlert(new AlertSnackbar(this));

        } else {

            try {

                // Already logged in?
                final MqttProfileClient profileClient = AmbulanceForegroundService.getProfileClient();
                Profile profile = profileClient.getProfile();
                if (profile != null) {

                    // Get user info & remove whitespace
                    final String username = usernameField.getText().toString().trim();

                    // Create intent
                    Intent intent = new Intent(LoginActivity.this,
                            MainActivity.class);

                    Log.i(TAG, "Starting MainActivity");

                    // Toast
                    Toast.makeText(LoginActivity.this,
                            getResources().getString(R.string.loginSuccessMessage, username),
                            Toast.LENGTH_SHORT).show();

                    // initiate MainActivity
                    startActivity(intent);

                    return;
                }

            } catch (AmbulanceForegroundService.ProfileClientException e) {

                // Initialize service to make sure it gets bound to service
                Intent intent = new Intent(this, AmbulanceForegroundService.class);
                intent.putExtra("ADD_STOP_ACTION", true);
                intent.setAction(AmbulanceForegroundService.Actions.START_SERVICE);
                startService(intent);

                // TODO: is this safe to do asynchronously?

            }

        }

        if (AmbulanceForegroundService.canUpdateLocation()) {

            // Toast to warn about check permissions
            Toast.makeText(LoginActivity.this, "Location permissions satisfied.", Toast.LENGTH_LONG).show();

        } else {

            // Alert to warn about check permissions
            new AlertSnackbar(LoginActivity.this)
                    .alert("Location permissions not satisfied. Expect limited functionality.");

        }

    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");

        // Disable login
        disableLogin();

        // Can updateAmbulance?
        if (AmbulanceForegroundService.canUpdateLocation()) {

            // Enable login
            enableLogin();

            // Check and request permissions to retrieve locations if necessary
        } else if (checkPermissions()) {

            // Otherwise check
            checkLocationSettings();

        } else {

            // requestPermissions calls checkLocationSettings if successful
            requestPermissions();

        }

    }

    @Override
    public void onClick(View view) {

        // Get user info & remove whitespace
        final String username = usernameField.getText().toString().trim();
        final String password = passwordField.getText().toString().trim();

        final String serverUri = serverURIs.get(serverField.getSelectedItemPosition());
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


        else {

            Log.d(TAG, "Will offer credentials");

            // Login at service
            Intent intent = new Intent(LoginActivity.this, AmbulanceForegroundService.class);
            intent.setAction(AmbulanceForegroundService.Actions.LOGIN);
            intent.putExtra("CREDENTIALS", new String[]{username, password, serverUri});

            // What to do when service completes?
            new OnServiceComplete(LoginActivity.this,
                    AmbulanceForegroundService.BroadcastActions.SUCCESS,
                    AmbulanceForegroundService.BroadcastActions.FAILURE,
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
                    .setAlert(new AlertSnackbar(LoginActivity.this));


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
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    // Request permission
                                    ActivityCompat.requestPermissions(LoginActivity.this,
                                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                            REQUEST_PERMISSIONS_REQUEST_CODE);
                                }
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
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {

                // If user interaction was interrupted, the permission request is cancelled and you
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
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        // Build intent that displays the App settings screen.
                                        Intent intent = new Intent();
                                        intent.setAction(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package",
                                                BuildConfig.APPLICATION_ID, null);
                                        intent.setData(uri);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    }
                                });
            }
        }
    }

    public void checkLocationSettings() {

        // Build settings client
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);

        // Check if the device has the necessary location settings.
        settingsClient.checkLocationSettings(AmbulanceForegroundService.getLocationSettingsRequest())
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        // enable location updates
                        AmbulanceForegroundService.setCanUpdateLocation(true);

                        // enable login
                        enableLogin();

                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
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
                                String errorMessage = "GPSLocation settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";

                                new AlertSnackbar(LoginActivity.this)
                                        .alert(errorMessage);

                                // disable location updates
                                AmbulanceForegroundService.setCanUpdateLocation(false);

                                // enable login
                                enableLogin();

                        }

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

}
