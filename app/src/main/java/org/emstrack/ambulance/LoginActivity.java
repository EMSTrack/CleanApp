package org.emstrack.ambulance;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.emstrack.ambulance.dialogs.AlertDialog;
import org.emstrack.ambulance.dialogs.AlertSnackbar;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.Credentials;
import org.emstrack.models.util.BroadcastActions;
import org.emstrack.models.util.BroadcastExtras;
import org.emstrack.models.util.OnServiceComplete;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = LoginActivity.class.getSimpleName();

    public static final String LOGOUT = "org.emstrack.ambulance.LoginActivity.LOGOUT";

    private SharedPreferences sharedPreferences;
    private Button loginSubmitButton;
    private TextView usernameField;
    private TextView passwordField;
    private Spinner serverField;
    private boolean logout;

    private ArrayAdapter<CharSequence> serverNames;
    private List<String> serverMqttURIs;
    private List<String> serverAPIURIs;

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

        // Log.d(TAG, "Populating server list");
        serverNames = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        serverMqttURIs = new ArrayList<>();
        serverAPIURIs = new ArrayList<>();

        // add select server message
        serverNames.add(this.getString(R.string.server_select));
        serverMqttURIs.add("");
        serverAPIURIs.add("");

        // Create server spinner
        serverField = findViewById(R.id.spinnerServer);
        serverNames.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serverField.setAdapter(serverNames);

        // Retrieving credentials
        sharedPreferences = getSharedPreferences(AmbulanceForegroundService.PREFERENCES_NAME, MODE_PRIVATE);

        // Retrieve past credentials
        usernameField.setText(sharedPreferences.getString(AmbulanceForegroundService.PREFERENCES_USERNAME, null));
        passwordField.setText(sharedPreferences.getString(AmbulanceForegroundService.PREFERENCES_PASSWORD, null));

        // Submit button
        loginSubmitButton = findViewById(R.id.buttonLogin);

    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");

        // enable login
        enableLogin();

    }

    public void enableLogin() {

        Log.d(TAG, "enableLogin");

        LoginActivity self = this;

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
                                getResources().getString(R.string.couldNotLogin, "unknown"),
                                Toast.LENGTH_SHORT).show();

                    }

                } else{

                    Log.i(TAG, "Could not find profile, starting service");

                    // Initialize service to make sure it gets bound to service
                    Intent intent = new Intent(LoginActivity.this,
                            AmbulanceForegroundService.class);
                    intent.putExtra("ADD_STOP_ACTION", true);
                    intent.setAction(AmbulanceForegroundService.Actions.START_SERVICE);

                    // Initialize service to make sure it gets bound to service
                    Intent serverIntent = new Intent(LoginActivity.this,
                            AmbulanceForegroundService.class);
                    serverIntent.setAction(AmbulanceForegroundService.Actions.GET_SERVERS);

                    new OnServiceComplete(LoginActivity.this,
                            BroadcastActions.SUCCESS,
                            BroadcastActions.FAILURE,
                            intent) {

                        @Override
                        public void onSuccess(Bundle extras) {
                            Log.i(TAG, "Successfully started service");
                        }

                    }
                            .setNext(new OnServiceComplete(LoginActivity.this,
                                    BroadcastActions.SUCCESS,
                                    BroadcastActions.FAILURE,
                                    serverIntent) {

                                @Override
                                public void onSuccess(Bundle extras) {

                                    Log.d(TAG, "Will set servers dropdown");

                                    // Retrieve list of servers
                                    AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
                                    List<String> serverList = appData.getServersList();
                                    Log.d(TAG, "Servers = " + serverList);

                                    // Populate server list
                                    // Log.d(TAG, "Populating server list");
                                    serverNames = new ArrayAdapter<>(self, android.R.layout.simple_spinner_item);
                                    serverMqttURIs = new ArrayList<>();
                                    serverAPIURIs = new ArrayList<>();

                                    // add select server message
                                    serverNames.add(self.getString(R.string.server_select));
                                    serverMqttURIs.add("");
                                    serverAPIURIs.add("");

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
                                    String serverMqttUri = sharedPreferences.getString(AmbulanceForegroundService.PREFERENCES_MQTT_SERVER, null);

                                    // set server item
                                    int serverPos = 0;
                                    if (serverMqttUri != null) {
                                        serverPos = serverMqttURIs.indexOf(serverMqttUri);
                                    }
                                    if (serverPos < 0)
                                        serverPos = 0;
                                    serverField.setSelection(serverPos);

                                    Log.d(TAG, "Will enable login button");

                                    // Enable login button
                                    loginSubmitButton.setOnClickListener(self);

                                }

                            })
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

    @Override
    public void onClick(View view) {

        // Get user info & remove whitespace
        final String username = usernameField.getText().toString().trim();
        final String password = passwordField.getText().toString().trim();

        final String serverUri = serverMqttURIs.get(serverField.getSelectedItemPosition());
        final String serverApiUri = serverAPIURIs.get(serverField.getSelectedItemPosition());
        Log.d(TAG, "Logging into server: " + serverUri);

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
                    .setFailureMessage(null)
                    .setAlert(new AlertDialog(LoginActivity.this,
                            getResources().getString(R.string.couldNotLoginUser, username)))
                    .start();

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

}
