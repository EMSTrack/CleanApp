package com.project.cruzroja.hospital;

import android.app.Activity;
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
                    alertEmptyLogin(LoginActivity.this, "username");
                }else if (password == null || password.isEmpty()){
                    alertEmptyLogin(LoginActivity.this, "password");
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
        alertDialog.setMessage("Please input a " + msg + ".  Field cannot be left blank.");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public void loginHospital(final String username, final String password) {
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
                    toast.setDuration(Toast.LENGTH_LONG);
                    Log.d(TAG, "Error parsing array");
                    return;
                }

                // Create hospital chooser
                createHospitalChooser(hospitalList);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "Message sent successfully");
            }
        });
    }

    private void createHospitalChooser(final ArrayList<Hospital> hospitals) {
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.select_dialog_singlechoice);

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Select Hospital");

        // Load up names
        for(int i = 0; i < hospitals.size(); i++) {
            arrayAdapter.add(hospitals.get(i).getName());
        }

        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                client.disconnect();
                dialog.dismiss();
                dialog.cancel();
            }
        });

        dialog.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                Hospital hospital = hospitals.get(i);
                Log.d(TAG, hospital.getId() + " ");
                String strName = hospital.getName(); //arrayAdapter.getItem(i);

                Intent dashboard = new Intent(getApplicationContext(), DashboardActivity.class);
                dashboard.putExtra("hospital_id", hospital.getId());
                dashboard.putExtra("hospital_name", hospital.getName());
                dashboard.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(dashboard);
            }
        });

        dialog.show();
    }
}
