    package com.antwish.povi.familyconnect;

    import android.app.Activity;
    import android.content.pm.PackageInfo;
    import android.content.pm.PackageManager;
    import android.content.pm.Signature;
    import android.support.v7.app.AlertDialog;
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
    import android.support.v7.app.AppCompatActivity;
    import android.support.v7.widget.Toolbar;
    import android.util.Base64;
    import android.util.Log;
    import android.view.View;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.TextView;

    import com.antwish.povi.server.User;
    import com.facebook.AccessToken;
    import com.facebook.CallbackManager;
    import com.facebook.FacebookCallback;
    import com.facebook.FacebookException;
    import com.facebook.FacebookSdk;
    import com.facebook.GraphRequest;
    import com.facebook.GraphResponse;
    import com.facebook.login.LoginBehavior;
    import com.facebook.login.LoginManager;
    import com.facebook.login.LoginResult;
    import com.google.android.gms.analytics.HitBuilders;
    import com.google.android.gms.analytics.Tracker;
    import com.linkedin.restli.common.HttpStatus;

    import org.json.JSONException;
    import org.json.JSONObject;

    import java.security.MessageDigest;
    import java.security.NoSuchAlgorithmException;
    import java.util.Arrays;
    import java.util.regex.Matcher;
    import java.util.regex.Pattern;

    public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "Login" ;
    public static final String POVI_TOKEN = "povi_token";
    public static final String POVI_USERID = "povi_userid";
    public static final String POVI_USERNAME = "povi_username";
    public static final String POVI_USERPIC = "povi_userpic";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private ProgressDialog loginDialog;
    private ProgressDialog resetDialog;
    private CallbackManager callbackManager;

    private Tracker tracker;

        private View view;

    Context context;

    private void facebookLogin(){
        LoginManager.getInstance().setLoginBehavior(LoginBehavior.NATIVE_WITH_FALLBACK);
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashActivity.disableStrictMode();
        super.onCreate(savedInstanceState);

        //String hash = printKeyHash(this);

        // Get tracker and signals the user entered the registration screen
        tracker = ((AnalyticsPoviApp) getApplication()).getTracker(
                AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
        tracker.setScreenName("Login screen");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

        context = this;

        // Initialize Facebook SDK!!!
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();


        setContentView(R.layout.activity_login);

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

        // Callback registration
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

            @Override
            public void onSuccess(LoginResult loginResult) {
                AccessToken accessToken = AccessToken.getCurrentAccessToken();
                // Login with POVI server
                final String poviToken = RestServer.facebookLogin(accessToken.getToken());
    //                final String poviToken = RestServer.facebookLogin(AccessToken.getCurrentAccessToken().getToken());
                if (poviToken != null) {

                    // Get user email
                    GraphRequest request = GraphRequest.newMeRequest(
                            loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                                @Override
                                public void onCompleted(JSONObject me, GraphResponse response) {
                                    if (response.getError() != null) {
                                        // handle error
                                    } else {
                                        String email = me.optString("email");
                                        String name = me.optString("name");
                                        String pic_url = null;
                                        try {
                                            pic_url = me.getJSONObject("picture").getJSONObject("data").getString("url");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        //String id = familyconnect.optString("id");
                                        // Save token and name
                                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                        SharedPreferences.Editor editor = sharedPref.edit();
                                        editor.putString(POVI_TOKEN, poviToken);
                                        editor.putString(POVI_USERID, email);
                                        editor.putString(POVI_USERNAME, name);
                                        editor.putString(POVI_USERPIC, pic_url);
                                        editor.commit();
                                        //Snackbar.make(findViewById(android.R.id.content), "Login succeded!", Snackbar.LENGTH_SHORT).show();

                                        // Send event to GA
                                        tracker.send(new HitBuilders.EventBuilder()
                                                .setCategory("Application")
                                                .setAction("Login")
                                                .setLabel("Facebook")
                                                .build());
                                        // Clear screen name
                                        tracker.setScreenName(null);

                                        Intent nextActivity = new Intent(getApplicationContext(), DashboardActivity.class);
                                        startActivity(nextActivity);
                                        finish();
                                    }
                                }
                            });
                    Bundle parameters = new Bundle();
                    parameters.putString("fields", "name,email,gender,birthday,picture");
                    request.setParameters(parameters);
                    request.executeAsync();

                } else
                    Snackbar.make(findViewById(android.R.id.content), "Login failed!", Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException e) {
                // Show error message
                Snackbar.make(findViewById(android.R.id.content), "Facebook login failed!", Snackbar.LENGTH_SHORT).show();
            }
        });

        final Button facebookRegistrationButton = (Button) findViewById(R.id.facebookLoginButton);
        facebookRegistrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                facebookLogin();
            }
        });

        final Button loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform "classic" login
                // Check e-mail pattern validity
                EditText emailEditText = (EditText) findViewById(R.id.email);
                String email = emailEditText.getText().toString();
                if (!isValidEmail(email)) {
                    emailEditText.setError("Invalid e-mail");
                    emailEditText.requestFocus();
                    return;
                }
                new LoginTask().execute();
            }
        });

        final android.support.v7.app.AlertDialog.Builder resetPasswordBuilder = new android.support.v7.app.AlertDialog.Builder(this);
        resetPasswordBuilder.setMessage("Reset password?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        new ResetTask().execute();
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        resetPasswordBuilder.create();

        final TextView passwordForgotten = (TextView) findViewById(R.id.passwordForgotten);
        passwordForgotten.setOnClickListener(
                new View.OnClickListener() {
                                                 @Override
                                                 public void onClick(View v) {
                                                     EditText emailEditText = (EditText) findViewById(R.id.email);
                                                     String email = emailEditText.getText().toString();
                                                     if(email == null || email.isEmpty()){
                                                         Snackbar.make(view, "Please provide an email address!", Snackbar.LENGTH_LONG).show();
                                                     } else {
                                                         // Open confirmation dialog
                                                         resetPasswordBuilder.show();
                                                     }
                                                 }
                                             });

                loginDialog = new ProgressDialog(this);
        loginDialog.setIndeterminate(true);
        loginDialog.setMessage("Please wait");
        loginDialog.setTitle("Login...");
        loginDialog.setCancelable(false);
        resetDialog = new ProgressDialog(this);
        resetDialog.setIndeterminate(true);
        resetDialog.setMessage("Please wait");
        resetDialog.setTitle("Password reset...");
        resetDialog.setCancelable(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private  class LoginTask extends AsyncTask<String, RestResponse<String>, RestResponse<String>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loginDialog.show();
        }

        @Override
        protected void onPostExecute(RestResponse<String> result) {
            super.onPostExecute(result);

            if (loginDialog != null)
                loginDialog.dismiss();

            // Directly go to the dashboard in case of success
            switch (result.getStatusCode()){
                case 200:

                // Send event to GA
                tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Login")
                        .setAction("Logged")
                        .setLabel("Classic")
                        .build());
                // Clear screen name
                tracker.setScreenName(null);

                Intent nextActivity = new Intent(getApplicationContext(), DashboardActivity.class);
                startActivity(nextActivity);
                finish();
                    break;
                case 500:
                    Snackbar.make(view, "Login failed!", Snackbar.LENGTH_LONG).show();
                    break;

                case 400:
                    Snackbar.make(view, result.getErrorMsg(), Snackbar.LENGTH_LONG).show();
                    break;
                case 404:
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setCancelable(false);
                    builder.setTitle("No internet connection").setMessage("Internet connection unavailable ").setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new LoginTask().execute();
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).show();
                    break;
            }
        }

        @Override
        protected RestResponse<String> doInBackground(String... params) {
            EditText emailEditText = (EditText) findViewById(R.id.email);
            EditText pwdEditText = (EditText) findViewById(R.id.password);
            String email = emailEditText.getText().toString();
            String pwd = pwdEditText.getText().toString();
            RestResponse<String> token = RestServer.loginEmail(email, pwd);
            if(token != null && token.getEntity() != null && token.getStatusCode() == HttpStatus.S_200_OK.getCode())
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

    private  class ResetTask extends AsyncTask<String, Integer, RestResponse<Boolean>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            resetDialog.show();
        }

        @Override
        protected void onPostExecute(RestResponse<Boolean> result) {
            super.onPostExecute(result);

            if (resetDialog != null)
                resetDialog.dismiss();

            if (result != null && result.getStatusCode() == HttpStatus.S_200_OK.getCode() && result.getEntity()){
                Snackbar.make(findViewById(android.R.id.content), "Password reset succeded!", Snackbar.LENGTH_SHORT).show();
            }
            else if(result.getStatusCode() == HttpStatus.S_400_BAD_REQUEST.getCode())
                Snackbar.make(findViewById(android.R.id.content), result.getErrorMsg(), Snackbar.LENGTH_SHORT).show();
            else
                Snackbar.make(findViewById(android.R.id.content), "Password reset failed!", Snackbar.LENGTH_SHORT).show();
        }

        @Override
        protected RestResponse<Boolean> doInBackground(String... params) {
            EditText emailEditText = (EditText) findViewById(R.id.email);
            String email = emailEditText.getText().toString();
            return RestServer.regenerateLoginPassword(email);
        }
    }

    // validating email id
    private boolean isValidEmail(String email) {
        String EMAIL_PATTERN = "\\b[A-Z0-9._%-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}\\b";
        Pattern pattern = Pattern.compile(EMAIL_PATTERN, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
/*
        public static String printKeyHash(Activity context) {
            PackageInfo packageInfo;
            String key = null;
            try {
                //getting application package name, as defined in manifest
                String packageName = context.getApplicationContext().getPackageName();

                //Retriving package info
                packageInfo = context.getPackageManager().getPackageInfo(packageName,
                        PackageManager.GET_SIGNATURES);

                Log.e("Package Name=", context.getApplicationContext().getPackageName());

                for (Signature signature : packageInfo.signatures) {
                    MessageDigest md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    key = new String(Base64.encode(md.digest(), 0));

                    // String key = new String(Base64.encodeBytes(md.digest()));
                    Log.e("Key Hash=", key);
                }
            } catch (PackageManager.NameNotFoundException e1) {
                Log.e("Name not found", e1.toString());
            }
            catch (NoSuchAlgorithmException e) {
                Log.e("No such an algorithm", e.toString());
            } catch (Exception e) {
                Log.e("Exception", e.toString());
            }

            return key;
        }*/

    }