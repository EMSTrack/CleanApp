package com.project.cruzroja.hospital;

import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.project.cruzroja.hospital.models.Hospital;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

import static com.project.cruzroja.hospital.R.id.parent;

public class LoginActivity extends AppCompatActivity {
    static EditText username_login;
    static EditText password_login;
//    private Spinner spinner;
    private static final String[] paths = {"Hospital General", "Clinica 2", "IMSS 1"};
    private MqttClient client;
    private static final String TAG = MqttClient.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.maintitlebar);
        View view = getSupportActionBar().getCustomView();

        ImageButton imageButton= (ImageButton)view.findViewById(R.id.AddBtn);

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Find username and password from layout
        username_login = (EditText) findViewById(R.id.username);
        password_login = (EditText) findViewById(R.id.password);

        // Submit button's click listener
        Button login_submit = (Button) findViewById(R.id.submit_login);
        login_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String user_text = username_login.getText().toString();
                String pass_text = password_login.getText().toString();
                user_text = user_text.replace(" " , "");
                pass_text = pass_text.replace(" " , "");
                loginHospital(user_text, pass_text);


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

    public void loginHospital(final String user_text, final String pass_text){
        client = MqttClient.getInstance(this);

        client.connect(user_text, pass_text, new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if(reconnect) {
                    Log.d(TAG, "Reconnected to broker");
                } else {
                    Log.d(TAG, "Connected to broker");
                }
                client.subscribeToTopic("user/" + user_text + "/hospital");
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "Connection to broker lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String tmp = new String(message.getPayload());
                Gson gson = new Gson();
                final ArrayList<Hospital> serverList = gson.fromJson(tmp, new TypeToken<List<Hospital>>(){}.getType());
                Log.d("LOGIN", "Message received: " + tmp);
                if(serverList.size() == 0 || serverList == null){
                    Log.d("LOGIN", "Error parsing array");
                }
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(LoginActivity.this);
                //builderSingle.setIcon(R.drawable.ic_launcher);
                builderSingle.setTitle("Select Hospital:-");

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(LoginActivity.this, android.R.layout.select_dialog_singlechoice);
                for(int i = 0; i < serverList.size(); i++){
                    Hospital tmp_hospital = serverList.get(i);
                    arrayAdapter.add(tmp_hospital.getName());
                }
                builderSingle.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                });

                builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {

                        final Hospital tmp_hospital = serverList.get(i);
                        String strName = tmp_hospital.getName(); //arrayAdapter.getItem(i);
                        AlertDialog.Builder builderInner = new AlertDialog.Builder(LoginActivity.this);
                        builderInner.setMessage(strName);
                        builderInner.setTitle("Your Selected Hospital is");
                        builderInner.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                                // Pass hospital and user info
                                Intent dashboard = new Intent(getApplicationContext(), DashboardActivity.class);
                                dashboard.putExtra("USER", user_text);
                                dashboard.putExtra("PASS", pass_text);
                                dashboard.putExtra("H_ID", tmp_hospital.getID());
                                dashboard.putExtra("H_NM", tmp_hospital.getName());
                                dashboard.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(dashboard);
                            }
                        });
                        builderInner.show();
                    }
                });
                builderSingle.show();
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "Message sent successfully");
            }
        });

    }


    public interface PostCommentResponseListener {
        public void requestStarted();
        public void requestCompleted();
        public void requestEndedWithError(VolleyError error);
    }

}
