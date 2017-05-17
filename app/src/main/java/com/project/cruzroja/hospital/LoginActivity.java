package com.project.cruzroja.hospital;

import android.content.Intent;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.util.*;

public class LoginActivity extends AppCompatActivity {
    private EditText username_login;
    private EditText password_login;
    private Spinner spinner;
    private static final String[] paths = {"Hospital General", "Clinica 2", "IMSS 1"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Find username and password from layout
        username_login = (EditText) findViewById(R.id.username);
        password_login = (EditText) findViewById(R.id.password);

        /*
         *  Populate Hospital drop-down list
         */
        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<String>adapter = new ArrayAdapter<String>(LoginActivity.this,
                android.R.layout.simple_spinner_item,paths);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Add listener to each item in drop down list
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener) this);



        // Submit button's click listener
        Button login_submit = (Button) findViewById(R.id.submit_login);
        login_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent dashboard = new Intent(LoginActivity.this, DashboardActivity.class);
                String user_text = username_login.getText().toString();
                String pass_text = password_login.getText().toString();
                startActivity(dashboard);
                finish();


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

    /*
     *  Action to do for each hospital selected
     */
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        // TODO: assign a string on each case
        switch (position) {
            case 0:
                // Whatever you want to happen when the first item gets selected
                break;
            case 1:
                // Whatever you want to happen when the second item gets selected
                break;
            case 2:
                // Whatever you want to happen when the thrid item gets selected
                break;

        }
    }

    public void loginHospital(){

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://cruzroja.ucsd.edu/auth/login";

        // Request a string response from the provided URL.
        StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                // TODO: Get response and set permissions
                System.out.println(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: Error when login
                System.out.println("Error on login");

            }
        }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<String, String>();
                String user_text = username_login.getText().toString();
                String pass_text = password_login.getText().toString();
                params.put("user",user_text);
                params.put("pass",pass_text);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                return params;
            }
        };

        // Add the request to the RequestQueue.
        queue.add(sr);
    }
    public interface PostCommentResponseListener {
        public void requestStarted();
        public void requestCompleted();
        public void requestEndedWithError(VolleyError error);
    }

}
