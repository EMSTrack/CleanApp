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

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.emstrack.mqtt.MqttClient;
import org.emstrack.mqtt.MqttCallback;
import org.emstrack.mqtt.MqttConnectCallback;
import org.emstrack.models.Profile;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public class LoginActivity extends AppCompatActivity {
    private SharedPreferences creds_prefs = null;
    SharedPreferences.Editor editor = null;
    private MqttClient client;

    private static final String TAG = MqttClient.class.getSimpleName();
    private static final String TAG_CHECK = "shared_preferences";

    private final String USER = "username";
    private final String PASS = "password";
    private final String CHECKBOX = "remember_me";

    private String username;
    private String password;

    /* strings */
    private String alert_error_title;
    private String alert_button_positive_text;
    private String please_wait;
    private String user_error;
    private String pass_error;
    private String invalid_creds;

    private ProgressDialog progressDialog;

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
        ImageView imageButton= (ImageView) view.findViewById(R.id.LogoutBtn);
        imageButton.setVisibility(View.GONE);

        progressDialog = new ProgressDialog(LoginActivity.this);

        // Find username and password from layout
        final EditText usernameField = (EditText) findViewById(R.id.username);
        final EditText passwordField = (EditText) findViewById(R.id.password);
        final CheckBox savedUsernameCheck = (CheckBox) findViewById(R.id.checkBox);

        // Get credentials
        creds_prefs = getSharedPreferences("org.emstrack.hospital", MODE_PRIVATE);
        editor = creds_prefs.edit();

        // Check if credentials are cached
        if (rememberUserEnabled()){
            Log.d(TAG_CHECK, "Remember user enabled, using credentials" + rememberUserEnabled());
            username = getStoredUser();
            usernameField.setText(username);
            savedUsernameCheck.setChecked(true);
        } else{
            Log.d(TAG_CHECK, "Remember user not enabled, asking for credentials");
        }

        // Submit button's click listener
        Button login_submit = (Button) findViewById(R.id.submit_login);
        login_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Get user info & remove whitspace
                username = usernameField.getText().toString().replace(" ", "");
                password = passwordField.getText().toString().replace(" ", "");

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
        return creds_prefs.getBoolean(CHECKBOX, false);

    }

    private String getStoredUser(){
        return creds_prefs.getString(USER, null);
    }

    private String getStoredPassword(){
        return creds_prefs.getString(PASS, null);
    }

    private void setStoredCredentials(String user, String pass){
        editor.putString(USER, user);
        editor.putString(PASS, pass);
        editor.putBoolean(CHECKBOX, true);
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

        client = MqttClient.getInstance(getApplicationContext()); // Use application context to tie service to app

        MqttConnectCallback connectCallback = new MqttConnectCallback() {
            @Override
            public void onFailure() {
                progressDialog.dismiss();
                alertEmptyLogin(LoginActivity.this, invalid_creds);
            }
        };

        client.connect(username, password, connectCallback, new MqttCallback() {

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                super.connectComplete(reconnect, serverURI);
                Log.d(TAG, "Subscribing to user/" + username + "/profile");
                client.subscribeToTopic("user/" + username + "/profile");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                // Check if remember me enabled
                CheckBox remember_box = (CheckBox) findViewById(R.id.checkBox);
                if(remember_box.isChecked()){
                    Log.d(TAG_CHECK, "Checkbox checked, storing credentials");
                    setStoredCredentials(username, password);
                } else {
                    clearStoredCredentials();
                }

                String json = new String(message.getPayload());
                Log.d(TAG, "Message received: '" + json + "'");

                // Parse message into list of hospitals
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
                Gson gson = gsonBuilder.create();

                Profile profile = gson.fromJson(json, Profile.class);
                Log.d(TAG, "Parsed profile: " + profile);

                // Error parsing or no hospitals
                if (profile == null) {
                    progressDialog.dismiss();
                    Log.d(TAG, "Error parsing array");
                    return;
                }

                // Set the static list and start the new HospitalEquipmentMetadata Intent
                HospitalListActivity.hospitals = profile.getHospitals();

                Intent hospitalIntent = new Intent(getApplicationContext(), HospitalListActivity.class);
                startActivity(hospitalIntent);

                // Clear the loading screen
                progressDialog.dismiss();

                // Clear the password field
                EditText passwordField = (EditText) findViewById(R.id.password);
                passwordField.setText("");
                passwordField.clearFocus();

                EditText usernameField = (EditText) findViewById(R.id.username);
                usernameField.clearFocus();

                Log.d(TAG, "Done with LoginActivity::messageArrived");
            }

        });
    }
}
