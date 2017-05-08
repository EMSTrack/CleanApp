package com.project.cruzroja.hospital;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText username_login = (EditText) findViewById(R.id.username);
        final EditText password_login = (EditText) findViewById(R.id.password);


        // Submit button's click listener
        Button login_submit = (Button) findViewById(R.id.submit_login);
        login_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent dashboard = new Intent(LoginActivity.this, DashboardActivity.class);
                startActivity(dashboard);

                String user_text = username_login.getText().toString();
                String pass_text = password_login.getText().toString();
            }
        });


    }
}
