package org.emstrack.ambulance;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import org.eclipse.paho.client.mqttv3.MqttException;

import org.emstrack.mqtt.MqttProfileCallback;
import org.emstrack.mqtt.MqttProfileClient;

public class LoginActivity extends AppCompatActivity {

    private final String PREFERENCES_USERNAME = "username";

    private static final String TAG = "LoginActvity";

    private SharedPreferences creds_prefs;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Create progress dialog
        progressDialog = new ProgressDialog(LoginActivity.this);

        // Find username and password from layout
        final EditText usernameField = findViewById(R.id.editUserName);
        final EditText passwordField = findViewById(R.id.editPassword);

        // Retrieving credentials
        creds_prefs = getSharedPreferences("org.emstrack.hospital", MODE_PRIVATE);

        // Retrieve past credentials
        usernameField.setText(creds_prefs.getString(PREFERENCES_USERNAME, null));

        // Submit button's click listener
        Button login_submit = (Button) findViewById(R.id.buttonLogin);
        login_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Get user info & remove whitspace
                String username = usernameField.getText().toString().trim();
                String password = passwordField.getText().toString().trim();

                if (username.isEmpty()) {
                    alertDialog(LoginActivity.this,
                            getResources().getString(R.string.alert_error_title),
                            getResources().getString(R.string.error_empty_username));
                } else if (password.isEmpty()) {
                    alertDialog(LoginActivity.this,
                            getResources().getString(R.string.alert_error_title),
                            getResources().getString(R.string.error_empty_password));
                } else {

                    // Show progress dialog
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progressDialog.setMessage(getResources().getString(R.string.message_please_wait));
                    progressDialog.setIndeterminate(true);
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.show();

                    // while attempting to login
                    loginHospital(username, password);
                }

            }
        });

        //allow keyboard to disappear on screen click
        findViewById(R.id.relativeLayout).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                return true;
            }
        });

    }

    public void loginHospital(final String username, final String password) {

        // Retrieve client
        final MqttProfileClient profileClient = ((AmbulanceApp) getApplication()).getProfileClient();

        // Set callback to be called after profile is retrieved
        profileClient.setCallback(new MqttProfileCallback() {

            @Override
            public void onSuccess() {

                // Get preferences editor
                SharedPreferences.Editor editor = creds_prefs.edit();

                // Save credentials
                Log.d(TAG, "Storing credentials");
                editor.putString(PREFERENCES_USERNAME, username);
                editor.apply();

                // Initiate new activity
                Intent hospitalIntent = new Intent(getApplicationContext(), AmbulanceListActivity.class);
                startActivity(hospitalIntent);

                // Clear the loading screen
                progressDialog.dismiss();

                // Clear the password field
                EditText passwordField = findViewById(R.id.editPassword);
                passwordField.setText("");
                passwordField.clearFocus();

                // Clear the username field
                EditText usernameField = findViewById(R.id.editUserName);
                usernameField.clearFocus();

                Log.d(TAG, "Done with LoginActivity.");

            }

            @Override
            public void onFailure(Throwable exception) {

                // Dismiss dialog
                progressDialog.dismiss();

                Log.d(TAG, "Failed to retrieve profile.");
                alertDialog(LoginActivity.this,
                        getResources().getString(R.string.alert_error_title),
                        exception.toString());

            }

        });

        try {

            // Attempt to connect
            profileClient.connect(username, password, new MqttProfileCallback() {

                @Override
                public void onSuccess() {
                    Log.d(TAG, "Successfully connected to broker.");
                }

                @Override
                public void onFailure(Throwable exception) {

                    // Dismiss dialog
                    progressDialog.dismiss();

                    Log.d(TAG, "Failed to connected to broker.");
                    String message;
                    if (exception instanceof MqttException) {
                        int reason = ((MqttException) exception).getReasonCode();
                        if (reason == MqttException.REASON_CODE_FAILED_AUTHENTICATION ||
                                reason == MqttException.REASON_CODE_NOT_AUTHORIZED ||
                                reason == MqttException.REASON_CODE_INVALID_CLIENT_ID)
                            message = getResources().getString(R.string.error_invalid_credentials);
                        else
                            message = String.format(getResources().getString(R.string.error_connection_failed),
                                    exception.toString());
                    } else {
                        message = exception.toString();
                    }

                    // Alert user
                    alertDialog(LoginActivity.this,
                            getResources().getString(R.string.alert_error_title),
                            message);
                }

            });

        } catch (MqttException exception) {

            // Alert user
            alertDialog(LoginActivity.this,
                    getResources().getString(R.string.alert_error_title),
                    String.format(getResources().getString(R.string.error_connection_failed),
                            exception.toString()));

        }

    }

    public void alertDialog(Activity activity, String title, String message) {

        AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,
                getResources().getString(R.string.alert_button_positive_text),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

}
