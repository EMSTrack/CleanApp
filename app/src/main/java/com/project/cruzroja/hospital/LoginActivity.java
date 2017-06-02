package com.project.cruzroja.hospital;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
    private static final String TAG = MqttClient.class.getSimpleName();

    private MqttClient client;
    private String user_error = "Please input a valid username.  Field cannot be left blank";
    private String pass_error = "Please input a valid password.  Field cannot be left blank";
    private String no_hospital_error = "No hospitals associated with this account!";
    public static ProgressDialog loading_dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Action bar
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.maintitlebar);

        View view = getSupportActionBar().getCustomView();
        ImageView imageButton= (ImageView) view.findViewById(R.id.LogoutBtn);
        imageButton.setVisibility(View.GONE);
//        imageButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                finish();
//            }
//        });

        // Find username and password from layout
        final EditText usernameButton = (EditText) findViewById(R.id.username);
        final EditText passwordButton = (EditText) findViewById(R.id.password);

        // Submit button's click listener
        Button login_submit = (Button) findViewById(R.id.submit_login);
        login_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Get user info & remove whitspace
                String username = usernameButton.getText().toString().replace(" ", "");
                String password = passwordButton.getText().toString().replace(" ", "");

                if (username == null || username.isEmpty() ){
                    alertEmptyLogin(LoginActivity.this, user_error);
                }else if (password == null || password.isEmpty()){
                    alertEmptyLogin(LoginActivity.this, pass_error);
                }
                else {

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
    public void alertEmptyLogin(Activity activity, String msg){
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
        loading_dialog = new ProgressDialog(LoginActivity.this); // this = YourActivity
        loading_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loading_dialog.setMessage("Please wait...");
        loading_dialog.setIndeterminate(true);
        loading_dialog.setCanceledOnTouchOutside(false);
        loading_dialog.show();
    }

    public void loginHospital(final String username, final String password) {
        showLoadingScreen();
        client = MqttClient.getInstance(getApplicationContext()); // Use application context to tie service to app
        client.passActivity(LoginActivity.this); // Pass activity for dialog builder, otherwise app crashes
        client.connect(username, password, new MqttCallbackExtended() {
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
                loading_dialog.dismiss();
                String json = new String(message.getPayload());
                Log.d(TAG, "Message received: " + json);

                // Parse message into list of hospitals
                Gson gson = new Gson();
                final ArrayList<Hospital> hospitalList = gson.fromJson(json,
                        new TypeToken<List<Hospital>>(){}.getType());

                // Error parsing or no hospitals
                if (hospitalList == null || hospitalList.size() == 0) {
                    Toast toast = new Toast(LoginActivity.this);
                    toast.setText("No hospitals associated with this account!");
                    alertEmptyLogin(LoginActivity.this, no_hospital_error);
                    toast.setDuration(Toast.LENGTH_LONG);
                    Log.d(TAG, "Error parsing array");
                    return;
                }

                // Set the static list and start the new Hospital Intent
                HospitalListActivity.hospitalList = hospitalList;

                Intent hospitalIntent = new Intent(getApplicationContext(), HospitalListActivity.class);
                startActivity(hospitalIntent);

                // Create hospital chooser
//                createHospitalChooser(hospitalList);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "Message sent successfully");
            }
        });
    }
}
