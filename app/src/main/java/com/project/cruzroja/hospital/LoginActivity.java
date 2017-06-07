package com.project.cruzroja.hospital;

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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.project.cruzroja.hospital.models.Hospital;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.*;

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
    private String user_error = "Please input a valid username.\nField cannot be left blank";
    private String pass_error = "Please input a valid password.\nField cannot be left blank";
    private String invalid_creds = "Please input valid login credentials.";
    private String no_hospital_error = "No hospitals associated with this account!";

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressDialog = new ProgressDialog(LoginActivity.this);

        // Get credentials
        creds_prefs = getSharedPreferences("com.project.cruzroja.hospital", MODE_PRIVATE);
        editor = creds_prefs.edit();

        // Check if credentials are cached
        if(rememberUserEnabled()){
            showLoadingScreen();
            Log.d(TAG_CHECK, "Remember user enabled, using credentials" + rememberUserEnabled());
            username = getStoredUser();
            password = getStoredPassword();
            Log.d(TAG_CHECK, "User: " + username + " Password: " + password);
            loginHospital(username, password);
        } else{
            Log.d(TAG_CHECK, "Remember user not enabled, asking for credentials");
        }

        setContentView(R.layout.activity_login);

        // Action bar
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.maintitlebar);

        View view = getSupportActionBar().getCustomView();
        ImageView imageButton= (ImageView) view.findViewById(R.id.LogoutBtn);
        imageButton.setVisibility(View.GONE);

        // Find username and password from layout
        final EditText usernameField = (EditText) findViewById(R.id.username);
        final EditText passwordField = (EditText) findViewById(R.id.password);

        // this = YourActivity

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
        //creds_prefs.edit().apply();
    }

    private void clearStoredCredentials(){
        editor.clear();
        editor.commit();
        //creds_prefs.edit().apply();
    }

    public void alertEmptyLogin(Activity activity, String msg) {
        AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public void showLoadingScreen(){
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("Please wait...");
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

        client.connect(username, password, connectCallback, new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if(reconnect)
                    Log.d(TAG, "Reconnected to broker");
                else
                    Log.d(TAG, "Connected to broker");
                client.subscribeToTopic("user/" + username + "/hospital");
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "Connection to broker lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // Check if remember me enabled
                CheckBox remember_box = (CheckBox) findViewById(R.id.checkBox);
                if(remember_box.isChecked()){
                    Log.d(TAG_CHECK, "Checkbox checked, storing credentials");
                    setStoredCredentials(username, password);
                }

                String json = new String(message.getPayload());
                Log.d(TAG, "Message received: " + json);

                // Parse message into list of hospitals
                Gson gson = new Gson();
                final ArrayList<Hospital> hospitalList = gson.fromJson(json,
                        new TypeToken<List<Hospital>>(){}.getType());

                // Error parsing or no hospitals
                if (hospitalList == null || hospitalList.size() == 0) {
                    progressDialog.dismiss();
                    Log.d(TAG, "Error parsing array");
                    return;
                }

                // Set the static list and start the new Hospital Intent
                HospitalListActivity.hospitalList = hospitalList;

                Intent hospitalIntent = new Intent(getApplicationContext(), HospitalListActivity.class);
                startActivity(hospitalIntent);

                // Clear the loading screen
                progressDialog.dismiss();

                // Clear the password field
                EditText passwordField = (EditText) findViewById(R.id.password);
                passwordField.setText("");

                clearFieldFocus();



            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "Message sent successfully");
            }

            public void clearFieldFocus() {
                System.out.println("Inside ClearFieldFocus");
                EditText usernameField = (EditText) findViewById(R.id.username);
                EditText passwordField = (EditText) findViewById(R.id.password);

                usernameField.clearFocus();
                passwordField.clearFocus();
            }
        });
    }
}
