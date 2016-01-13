package com.antwish.povi.familyconnect;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.antwish.povi.server.User;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.linkedin.restli.common.HttpStatus;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegistrationActivity extends AppCompatActivity {
    private static final String TAG = "Registration" ;

    private static final String POVI_TOKEN = "povi_token";
    private static final String POVI_USERID = "povi_userid";
    private static final String POVI_USERNAME = "povi_username";

    private Tracker tracker;

    private ProgressDialog registrationDialog;

    private Context context;

    private View view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;

        // Get tracker and signals the user entered the registration screen
        tracker = ((AnalyticsPoviApp) getApplication()).getTracker(
                AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
        tracker.setScreenName("Registration screen");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

        setContentView(R.layout.activity_registration);

        view = findViewById(android.R.id.content);

        // Enable action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        // Add top padding if required
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT){
            final float scale = getResources().getDisplayMetrics().density;
            int top = (int) (30 * scale + 0.5f);
            toolbar.setPadding(0,top,0,0);
        }
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        TextView textView =(TextView)findViewById(R.id.termofuseText);
        textView.setClickable(true);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        //String text = "<a href='http://www.google.com'> Google </a>";

        String text="By signing up, you agree to Povi Family Connect's <a href='http://www.povi.me/terms-and-conditions.html'> <b>Terms and Conditions of Use</b></a> and <a href='http://www.povi.me/privacy-policy.html'> <b>Privacy Policy</b></a>";
        textView.setText(Html.fromHtml(text));

        final Button registrationButton = (Button) findViewById(R.id.registerButton);
        registrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check e-mail pattern validity
                final EditText emailText = (EditText) findViewById(R.id.email);
                final EditText passwordText = (EditText) findViewById(R.id.password);
                final EditText password2Text = (EditText) findViewById(R.id.retypePassword);
                final EditText nameText = (EditText) findViewById(R.id.name);
                String email = emailText.getText().toString();
                String password = passwordText.getText().toString();
                String password2 = password2Text.getText().toString();
                if (!isValidEmail(email)) {
                    emailText.setError("Invalid e-mail");
                    emailText.requestFocus();
                    return;
                }

                // Check password validity
                if (password.compareTo(password2) != 0) {
                    password2Text.setError("Password not matching");
                    password2Text.requestFocus();
                    return;
                }
                new RegistrationTask().execute();
            }
        });

        registrationDialog = new ProgressDialog(this);
        registrationDialog.setIndeterminate(true);
        registrationDialog.setMessage("Please wait");
        registrationDialog.setTitle("Registration...");
        registrationDialog.setCancelable(false);
    }

    // validating email id
    private boolean isValidEmail(String email) {
        String EMAIL_PATTERN = "\\b[A-Z0-9._%-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}\\b";
        Pattern pattern = Pattern.compile(EMAIL_PATTERN, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    private  class RegistrationTask extends AsyncTask<String, RestResponse<String>, RestResponse<String>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            registrationDialog.show();
        }

        @Override
        protected void onPostExecute(RestResponse<String> result) {
            super.onPostExecute(result);

            if (registrationDialog != null)
                registrationDialog.dismiss();

            // Directly go to the dashboard in case of success
            switch (result.getStatusCode()){
                case 201:
                    // Send event to GA
                    tracker.send(new HitBuilders.EventBuilder()
                            .setCategory("Registration")
                            .setAction("Registered")
                            .setLabel("User")
                            .build());

                    Intent nextActivity = new Intent(getApplicationContext(), DashboardActivity.class);
                    startActivity(nextActivity);
                    finish();

                case 400:
                    Snackbar.make(view, result.getErrorMsg(), Snackbar.LENGTH_LONG).show();
                    break;
                case 404:
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setCancelable(false);
                    builder.setTitle("No internet connection").setMessage("Internet connection unavailable ").setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new RegistrationTask().execute();
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).show();
                    break;
                default:
                    Snackbar.make(view, "Registration failed", Snackbar.LENGTH_LONG).show();
                    break;
            }
        }

        @Override
        protected RestResponse<String> doInBackground(String... params) {
            // Send registration data to server and validate it
            final EditText emailText = (EditText) findViewById(R.id.email);
            final EditText passwordText = (EditText) findViewById(R.id.password);
            final EditText password2Text = (EditText) findViewById(R.id.retypePassword);
            final EditText nameText = (EditText) findViewById(R.id.name);
            String email = emailText.getText().toString();
            String password = passwordText.getText().toString();
            String password2 = password2Text.getText().toString();
            String name = nameText.getText().toString();

            String data = email + password;
            MessageDigest digest = null;
            try {
                digest = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            digest.reset();
            try {
                digest.update(data.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String hash = new BigInteger(1, digest.digest()).toString(16);

            //String res = RestServer.registerNewAccount(email, hash, name, phone, address, birthdateTimestamp);
            RestResponse<String> token = RestServer.registerNewAccount(email, hash, name, "555", null, 0);
            if(token != null && token.getEntity() != null && token.getStatusCode() == HttpStatus.S_201_CREATED.getCode())
            {
                // Get user profile
                User user = RestServer.getUserProfile(token.getEntity());
                if (user != null) {
                    // Get username and e-mail
                    // Save token
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(POVI_TOKEN, token.getEntity());
                    editor.putString(POVI_USERID, user.getEmail());
                    editor.putString(POVI_USERNAME, user.getName());
                    editor.commit();
                }
            }

            return token;
        }
    }
}
