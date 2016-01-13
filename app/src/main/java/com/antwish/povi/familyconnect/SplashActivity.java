package com.antwish.povi.familyconnect;

import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * The SplashActivity class creates an animation by showing a sequence of images in order
 * with an AnimationDrawable {@link AnimationDrawable}. An animation defined in XML
 * {@code splash_animation} shows a sequence of images like a film. We set the oneshot parameter
 * to true to cycle through the list of images once.
 * Created by aditya on 5/6/15.
 */
public class SplashActivity extends AppCompatActivity {
    private AnimationDrawable frameAnimation;
    private boolean running = false;
    private Context context;

    /**
     * Duration of display before login activity is called
     */
    private final int SPLASH_DISPLAY_LENGTH = 4500;

    /**
     * The ImageView element which is used to bind to the ImageView in
     * the splash screen activity {@code activity_splash_screen}
     */
    private ImageView mImageView;

    private Tracker tracker;

    public static void disableStrictMode() {
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
    }
    // Called when the activity is first created
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;

        // Get tracker and signals the user entered the registration screen
        tracker = ((AnalyticsPoviApp) getApplication()).getTracker(
                AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
        tracker.setScreenName("Splash screen");
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Application")
                .setAction("Start")
                .setLabel("App_started")
                .build());

        disableStrictMode();

        setContentView(R.layout.activity_splash);

        // Assign view elements to variables
        mImageView = (ImageView) findViewById(R.id.splashImageView);

        // Set ImageView background to our AnimationDrawable XML resource.
        //mImageView.setImageResource(R.drawable.splash_animation);

        // Get the background, which has been compiled into an AnimationDrawable object
        frameAnimation = (AnimationDrawable) mImageView.getDrawable();

        // Start the animation (Oneshot playback)
        frameAnimation.setOneShot(true);
        running = true;
        frameAnimation.start();

        // Start user login verification if it's not the first time
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean firstExectution = sharedPref.getBoolean("first_execution", true);
        Intent nextActivity;
        if (firstExectution) {
                    /*
         * New handler to start main activity and close
         * this splash screen after SPLASH_DISPLAY_LENGTH
         */
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent nextActivity = new Intent(getApplicationContext(), TutorialActivity.class);
                    startActivity(nextActivity);
                    finish();
                }
            }, SPLASH_DISPLAY_LENGTH);
        } else {
            new VerifyLoginTask().execute();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    running = false;
                }
            }, SPLASH_DISPLAY_LENGTH);
        }
    }

    private  class VerifyLoginTask extends AsyncTask<String, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
                    // Directly go to the dashboard in case of success
                    Intent nextActivity;
                    switch (result){
                        case 0:
                            tracker.send(new HitBuilders.EventBuilder()
                                    .setCategory("Application")
                                    .setAction("Login")
                                    .setLabel("Automatic_login")
                                    .build());
                        nextActivity = new Intent(getApplicationContext(), DashboardActivity.class);
                            startActivity(nextActivity);
                            finish();
                            break;
                        case -1:
                            nextActivity = new Intent(getApplicationContext(), WelcomeActivity.class);
                            startActivity(nextActivity);
                            finish();
                        case 1:
                        case 2:
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setCancelable(false);
                            builder.setTitle("No internet connection").setMessage("The app will be closed now").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            }).show();
                    }
        }

        @Override
        protected Integer doInBackground(String... params) {
            // Recall current token
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String currentToken = sharedPref.getString("povi_token", "");
            int result = RestServer.validateToken(currentToken);
            while (running) {
                try {
                   Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return result;
        }
    }
}