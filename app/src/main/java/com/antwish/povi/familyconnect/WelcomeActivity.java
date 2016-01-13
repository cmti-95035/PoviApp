package com.antwish.povi.familyconnect;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class WelcomeActivity extends AppCompatActivity {
    private static final String TAG = "Welcome" ;
    private ProgressDialog verifyLoginDialog;
    private Button loginButton;
    private Button registerButton;
    private View view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);

        view = findViewById(R.id.view);

        verifyLoginDialog = new ProgressDialog(this);
        verifyLoginDialog.setIndeterminate(true);
        verifyLoginDialog.setMessage("Please wait");
        verifyLoginDialog.setTitle("User verification...");
        verifyLoginDialog.setCancelable(false);

        loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent nextActivity = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(nextActivity);
            }
        });

        registerButton = (Button) findViewById(R.id.registerButton);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent nextActivity = new Intent(getApplicationContext(), RegistrationActivity.class);
                startActivity(nextActivity);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}