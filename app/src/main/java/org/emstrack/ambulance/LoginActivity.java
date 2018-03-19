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

                    // Login at foreground activity
                    Intent intent = new Intent(LoginActivity.this, AmbulanceForegroundService.class);
                    intent.setAction(AmbulanceForegroundService.Actions.LOGIN);
                    intent.putExtra("CREDENTIALS", new String[] { username, password });
                    startService(intent);

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
