package com.antwish.povi.familyconnect;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;

public class DashboardActivity extends AppCompatActivity
        implements OnTitleChangeListener,
        ChildrenFragment.OnChildUpdateListener,
        ChildrenFragment.OnChildrenToolbarElevationListener,
        BeatsFragment.OnBeatsToolbarElevationListener,
        ProfileFragment.OnProfileUpdateListener,
        TreasureChestFragment.OnBeatsToolbarElevationListener,
        SubscriptionFragment.OnSubscriptionsToolbarElevationListener {

    private static final String POVI_TOKEN = "povi_token";
    private static final String POVI_USERID = "povi_userid";
    private static final String POVI_USERNAME = "povi_username";
    private static final String POVI_USERPIC = "povi_userpic";
    private static final String POVI_NOTIFICATION_SCHEDULE = "povi_notification_schedule";

    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private int mCurrentSelectedPosition = 0;
    private Runnable mPendingRunnable;
    private Handler mHandler = new Handler();
    private Bitmap userPicture = null;
    private CardView cardToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashActivity.disableStrictMode();
        super.onCreate(savedInstanceState);

        // Get tracker and signals the user entered the registration screen
        //Tracker tracker = ((AnalyticsPoviApp) getApplication()).getTracker(
        //        AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
        //tracker.setScreenName("Dashboard screen");
        //tracker.send(new HitBuilders.ScreenViewBuilder().build());

        setContentView(R.layout.activity_dashboard);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {

            }

            @Override
            public void onDrawerClosed(View drawerView) {
                // Start runnable
                if (mPendingRunnable != null) {
                    mHandler.post(mPendingRunnable);
                    mPendingRunnable = null;
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

        setUpToolbar();
        setUpNavDrawer();

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Log out current user?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        logout();
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        builder.create();

        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                final MenuItem selection = menuItem;
                mPendingRunnable = new Runnable() {
                    @Override
                    public void run() {
                        selection.setChecked(true);
                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction trans = fragmentManager.beginTransaction();
                        switch (selection.getItemId()) {
//                            case R.id.userProfile: {
//                                ProfileFragment fragment = new ProfileFragment();
//                                trans.replace(R.id.nav_contentframe, fragment);
//                                mCurrentSelectedPosition = 0;
//                            }
                            case R.id.conversationStarter: {
                                trans.replace(R.id.nav_contentframe, new StoryFragment());
                                mCurrentSelectedPosition = 0;
                            }
                            break;
//                            case R.id.children: {
//                                ChildrenFragment fragment = new ChildrenFragment();
//                                trans.replace(R.id.nav_contentframe, fragment, "children_fragment");
//                                mCurrentSelectedPosition = 1;
//                            }
                            case R.id.journal: {
                                trans.replace(R.id.nav_contentframe, new JournalFragment());
                                mCurrentSelectedPosition = 1;
                            }
                            break;
//                            case R.id.tip: {
//                                //TipjarFragment fragment = new TipjarFragment();
//                                BeatsFragment fragment = new BeatsFragment();
//                                trans.replace(R.id.nav_contentframe, fragment);
//                                mCurrentSelectedPosition = 2;
//                            }
                            case R.id.subscription: {
                                trans.replace(R.id.nav_contentframe, new SubscriptionFragment2());
                                mCurrentSelectedPosition = 2;
                            }
                            break;
//                            case R.id.treasurechest: {
//                                TreasureChestFragment fragment = new TreasureChestFragment();
//                                trans.replace(R.id.nav_contentframe, fragment);
//                                mCurrentSelectedPosition = 3;
//                            }
                            case R.id.settings: {
                                trans.replace(R.id.nav_contentframe, new SettingsFragment());
                                mCurrentSelectedPosition = 3;
                            }
                            break;
//
//                            case R.id.help: {
//                                /*HelpFragment fragment = new HelpFragment();
//                                trans.replace(R.id.nav_contentframe, fragment);
//                                mCurrentSelectedPosition = 4;*/
//                                openPoviPage();
//                            }
//                            break;
//                            case R.id.settings: {
//                                trans.replace(R.id.nav_contentframe, new SettingsFragment());
//                                mCurrentSelectedPosition = 5;
//                            }
//                            break;
//                            case R.id.logout: {
//                                builder.show();
//                                mCurrentSelectedPosition = 6;
//                            }
//                            break;
//                            case R.id.povistory: {
//                                mCurrentSelectedPosition = 7;
//                                final Intent intent = new Intent(getApplicationContext(), ScanningActivity.class);
//                                intent.putExtra(PeripheralActivity.EXTRAS_TYPE, "play");
//                                startActivity(intent);
//                            }
//                            break;
//                            case R.id.uploadpovistory: {
//                                mCurrentSelectedPosition = 8;
//                                final Intent intent = new Intent(getApplicationContext(), ScanningActivity.class);
//                                intent.putExtra(PeripheralActivity.EXTRAS_TYPE, "upload");
//                                startActivity(intent);
//                            }
//                            break;
                        }

                            trans.addToBackStack(null);
                            trans.commit();
                    }
                };

                // Set notification time
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                boolean notificationScheduled = settings.getBoolean(POVI_NOTIFICATION_SCHEDULE, false);

                if (!notificationScheduled) {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 18);
                    cal.set(Calendar.MINUTE, 2);
                    long notificationTime = settings.getLong("notification_time", cal.getTimeInMillis());
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    long journalTime = settings.getLong("journal_time", cal.getTimeInMillis());

                    cal.setTimeInMillis(notificationTime);

                    // set the alarm manager
                    AlarmUtils.scheduleAlarm(
                            getApplicationContext(),
                            AlarmUtils.calculateTimeDifference(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)),
                            AlarmUtils.NotificationType.TIPTIME
                    );
                    cal.setTimeInMillis(journalTime);

                    // set the alarm manager
                    AlarmUtils.scheduleAlarm(
                            getApplicationContext(),
                            AlarmUtils.calculateTimeDifference(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)),
                            AlarmUtils.NotificationType.JOURNALTIME
                    );

                    // this sets the notification schedules to the default time and it should only be set once
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(POVI_NOTIFICATION_SCHEDULE, true);
                    editor.commit();
                }

                // close drawer if open
                mDrawerLayout.closeDrawers();
                return true;
            }
        });

//        FragmentManager fragmentManager = getFragmentManager();
//        SubscriptionFragment fragment = new SubscriptionFragment();
//        FragmentTransaction trans = fragmentManager.beginTransaction();
//        trans.replace(R.id.nav_contentframe, fragment);
//        trans.commit();
//        setTitle("Subscription");

        FragmentManager fragmentManager = getFragmentManager();
        StoryFragment fragment = new StoryFragment();
        FragmentTransaction trans = fragmentManager.beginTransaction();
        trans.replace(R.id.nav_contentframe, fragment);
        trans.commit();
        setTitle("Conversation Starter");

        // Set drawer user's email and name
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String email = sharedPref.getString(POVI_USERID, "povi@povi.me");
//        String email = sharedPref.getString(POVI_USERID, null);
        String name = sharedPref.getString(POVI_USERNAME, "James");
//        String name = sharedPref.getString(POVI_USERNAME, null);
        TextView headerEmail = (TextView) findViewById(R.id.headerEmail);
        headerEmail.setText(email);
        TextView headerName = (TextView) findViewById(R.id.headerName);
        headerName.setText(name);
    }


    private void setUpToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        cardToolbar = (CardView) findViewById(R.id.cardToolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
        }
    }

    private void setUpNavDrawer() {
        if (mToolbar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            mToolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
            });

            // Show navigation drawer the first time
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            boolean firstExectution = sharedPref.getBoolean("first_execution_drawer", true);
            if (firstExectution) {
                mDrawerLayout.openDrawer(GravityCompat.START);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("first_execution_drawer", false);
                editor.commit();
            }
        }
    }

    private void logout() {
        // Logout
        // Recall current token
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String currentToken = sharedPref.getString("povi_token", "");
        boolean res = RestServer.logout(currentToken);
        if (res) {
            // Logout from Facebook
            FacebookSdk.sdkInitialize(getApplicationContext());
            LoginManager.getInstance().logOut();
            // Clear token and userid
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(POVI_USERID, null);
            editor.putString(POVI_USERNAME, null);
            editor.putString(POVI_TOKEN, null);
            editor.putString(POVI_USERPIC, null);
            editor.commit();

            // Send event to GA
            Tracker tracker = ((AnalyticsPoviApp) getApplication()).getTracker(
                    AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Login")
                    .setAction("Logout")
                    .setLabel("User")
                    .build());

            // Logout succeded, come back to the login screen
            Intent i = new Intent(getApplicationContext(), WelcomeActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        } else {
            Snackbar.make(mDrawerLayout, "Logout failed!", Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * Goes through backstack on pressing the back button
     */
    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();

        } else {
            super.onBackPressed();
        }
        // close drawer if open
        mDrawerLayout.closeDrawers();
    }

    @Override
    public void onTitleChangeListener(String title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void onChildUpdateListener() {
        // Tell fragment to refresh children list
        ChildrenFragment fragment = (ChildrenFragment) getFragmentManager().findFragmentByTag("children_fragment");
        if (fragment != null)
            fragment.refreshChildren();
    }

    @Override
    public void onToolbarElevationListener(float elevation) {
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
        //mToolbar.setElevation(elevation);
        cardToolbar.setCardElevation(elevation);
        //}

    }

    @Override
    public void onProfileUpdateListener() {
        // Retrieve user's e-mail and password from shared preferences and update the navigation drawer header
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String email = sharedPref.getString(POVI_USERID, null);
        String name = sharedPref.getString(POVI_USERNAME, null);
        TextView headerEmail = (TextView) findViewById(R.id.headerEmail);
        headerEmail.setText(email);
        TextView headerName = (TextView) findViewById(R.id.headerName);
        headerName.setText(name);
    }

    private class ProfileImageTask extends AsyncTask<String, Integer, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            ImageView avatar = (ImageView) findViewById(R.id.headerAvatar);
            // Directly go to the dashboard in case of success
            if (result) {

                avatar.setImageBitmap(userPicture);
            } else {
                // Set POVI avatar as default
                avatar.setImageResource(R.drawable.navdrawer_icon);
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String picUrl = sharedPref.getString(POVI_USERPIC, null);
            try {
                URL url = new URL(picUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myPic = BitmapFactory.decodeStream(input);


                // Get the screen's density scale
                final float scale = getResources().getDisplayMetrics().density;
// Convert the dps to pixels, based on density scale
                int sz = (int) (64 * scale + 0.5f);
                Bitmap myPicScaled = Bitmap.createScaledBitmap(myPic, sz, sz, true);
                userPicture = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(userPicture);
                Shader shader = new BitmapShader(myPicScaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                Paint paint = new Paint();
                paint.setShader(shader);
                paint.setFilterBitmap(true);
                paint.setAntiAlias(true);
                canvas.drawColor(Color.TRANSPARENT);
                canvas.drawCircle(sz / 2, sz / 2, userPicture.getWidth() / 2, paint);

                return true;
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    private void openPoviPage() {
        Tracker tracker = ((AnalyticsPoviApp) getApplication()).getTracker(
                AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
        // Send event to GA
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Help")
                .setAction("Click")
                .setLabel("Website")
                .build());
        // Clear screen name
        tracker.setScreenName(null);

        Uri webpage = Uri.parse("http://www.povi.me");
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }
}