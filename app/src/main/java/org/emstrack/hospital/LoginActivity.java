package org.emstrack.hospital;

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

    private static final String TAG = "LoginActvity";
    private static final String TAG_CHECK = "shared_preferences";

    private SharedPreferences creds_prefs = null;
    SharedPreferences.Editor editor = null;

    private final String PREFERENCES_USERNAME = "username";
    private final String PREFERENCES_PASSWORD = "password";
    private final String PREFERENCES_REMEMBER_ME = "remember_me";

    private ProgressDialog progressDialog;

    /* strings */
    private String alert_error_title;
    private String alert_button_positive_text;
    private String please_wait;
    private String user_error;
    private String pass_error;
    private String invalid_creds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        alert_error_title = getResources().getString(R.string.alert_error_title);
        alert_button_positive_text = getResources().getString(R.string.alert_button_positive_text);
        please_wait = getResources().getString(R.string.please_wait);
        user_error = getResources().getString(R.string.user_error);
        pass_error = getResources().getString(R.string.pass_error);
        invalid_creds = getResources().getString(R.string.invalid_creds);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Action bar
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.maintitlebar);
        View view = getSupportActionBar().getCustomView();
        ImageView imageButton= view.findViewById(R.id.LogoutBtn);
        imageButton.setVisibility(View.GONE);

        progressDialog = new ProgressDialog(LoginActivity.this);

        // Find username and password from layout
        final EditText usernameField = findViewById(R.id.username);
        final EditText passwordField = findViewById(R.id.password);
        final CheckBox savedUsernameCheck = findViewById(R.id.checkBox);

        // Get credentials
        creds_prefs = getSharedPreferences("org.emstrack.hospital", MODE_PRIVATE);
        editor = creds_prefs.edit();

        // Check if credentials are cached
        if (rememberUserEnabled()){
            Log.d(TAG_CHECK, "Remember user enabled, using credentials" + rememberUserEnabled());
            usernameField.setText(getStoredUser());
            savedUsernameCheck.setChecked(true);
        } else{
            Log.d(TAG_CHECK, "Remember user not enabled, asking for credentials");
        }

        // Submit button's click listener
        Button login_submit = findViewById(R.id.submit_login);
        login_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Get user info & remove whitspace
                String username = usernameField.getText().toString().trim();
                String password = passwordField.getText().toString().trim();

                if (username == null || username.isEmpty()) {
                    alertEmptyLogin(LoginActivity.this, user_error);
                } else if (password == null || password.isEmpty()) {
                    alertEmptyLogin(LoginActivity.this, pass_error);
                } else {
                    showLoadingScreen();
                    loginHospital(username, password);
                }

            }
        });

        // Code to hide keyboard if user clicks out of window
        findViewById(R.id.relativeLayout).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                return true;
            }
        });
    }

    private boolean rememberUserEnabled(){
        return creds_prefs.getBoolean(PREFERENCES_REMEMBER_ME, false);
    }

    private String getStoredUser(){
        return creds_prefs.getString(PREFERENCES_USERNAME, null);
    }

    private String getStoredPassword(){
        return creds_prefs.getString(PREFERENCES_PASSWORD, null);
    }

    private void setStoredCredentials(String username, String password){
        editor.putString(PREFERENCES_USERNAME, username);
        editor.putString(PREFERENCES_PASSWORD, password);
        editor.putBoolean(PREFERENCES_REMEMBER_ME, true);
        editor.commit();
    }

    private void clearStoredCredentials(){
        editor.clear();
        editor.commit();
    }

    public void alertEmptyLogin(Activity activity, String msg) {
        AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
        alertDialog.setTitle(alert_error_title);
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, alert_button_positive_text,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public void showLoadingScreen(){
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(please_wait);
        progressDialog.setIndeterminate(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    public void loginHospital(final String username, final String password) {

        // Retrieve client
        final MqttProfileClient profileClient = ((HospitalApp) getApplication()).getProfileClient();
        try {

            profileClient.setCallback(new MqttProfileCallback() {
                @Override
                public void onSuccess() {

                    // Check if remember me enabled
                    CheckBox remember_box = findViewById(R.id.checkBox);
                    if(remember_box.isChecked()){
                        Log.d(TAG_CHECK, "Checkbox checked, storing credentials");
                        setStoredCredentials(username, password);
                    } else {
                        clearStoredCredentials();
                    }

                    // Initiate new activity
                    Intent hospitalIntent = new Intent(getApplicationContext(), HospitalListActivity.class);
                    startActivity(hospitalIntent);

                    // Clear the loading screen
                    progressDialog.dismiss();

                    // Clear the password field
                    EditText passwordField = findViewById(R.id.password);
                    passwordField.setText("");
                    passwordField.clearFocus();

                    EditText usernameField = findViewById(R.id.username);
                    usernameField.clearFocus();

                    Log.d(TAG, "Done with LoginActivity.");

                }

                @Override
                public void onFailure(Throwable exception) {

                    // TODO: Handle failure

                }
            });
            profileClient.connect(username, password, new MqttProfileCallback() {

                @Override
                public void onSuccess() {
                    Log.d(TAG, "Successfully connected to broker.");
                }

                @Override
                public void onFailure(Throwable exception) {
                    progressDialog.dismiss();
                    alertEmptyLogin(LoginActivity.this, invalid_creds);
                    Log.d(TAG, "Failed to connected to broker.");
                }

            });

        } catch (MqttException e) {
            Log.d(TAG, "Could not retrieve profile");
        }

    }
}
