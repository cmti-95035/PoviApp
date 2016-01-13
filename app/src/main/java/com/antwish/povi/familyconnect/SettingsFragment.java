package com.antwish.povi.familyconnect;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class SettingsFragment extends PreferenceFragment {
    private OnTitleChangeListener titleChangeListener;

    private Tracker tracker;

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        return fragment;
    }

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get tracker and signals the user entered the registration screen
        tracker = ((AnalyticsPoviApp) getActivity().getApplication()).getTracker(
                AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
        tracker.setScreenName("Settings screen");
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

        // Add preference change listener and update initial summaries
        Preference pref = findPreference("notification_status");
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateNotificationStatus(preference, (boolean) newValue);
                return true;
            }
        });
        boolean notificationStatus = settings.getBoolean("notification_status", true);
        updateNotificationStatus(pref, notificationStatus);


        pref = findPreference("notification_time");
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateNotificationTime(preference, (long) newValue);
                return true;
            }
        });

        // Set default time
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 18);
        cal.set(Calendar.MINUTE, 0);
        long notificationTime = settings.getLong("notification_time", cal.getTime().getTime());
        updateNotificationTime(pref, notificationTime);

        pref = findPreference("journal_time");
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateJournalTime(preference, (long) newValue);
                return true;
            }
        });
        cal.set(Calendar.HOUR_OF_DAY, 22);
        cal.set(Calendar.MINUTE, 0);
        long journalTime = settings.getLong("journal_time", cal.getTime().getTime());
        updateJournalTime(pref, journalTime);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
       /* if(v != null) {
            LinearLayout ll = (LinearLayout) v;
            ImageView mockToolbar = new ImageView(getActivity());
            //ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(getActivity(),)
            //params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            //params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics());
            //mockToolbar.setLayoutParams(params);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                mockToolbar.setElevation((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()));
            mockToolbar.setBackgroundColor(getResources().getColor(R.color.amber));
            ll.addView(mockToolbar, 0);
            ViewGroup.LayoutParams params = ll.getChildAt(0).getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
            ll.getChildAt(0).setLayoutParams(params);
            //ListView lv = (ListView) v.findViewById(android.R.id.list);
            //lv.setPadding(10, 10, 10, 10);
        }*/
        view.setPadding(0,(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56, getResources().getDisplayMetrics()),0,0);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            titleChangeListener = (OnTitleChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTitleChangeListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        titleChangeListener = null;
    }

    @Override
    public void onResume(){
        super.onResume();
        if (titleChangeListener != null)
            titleChangeListener.onTitleChangeListener("Settings");
    }

    public final void updateNotificationStatus(Preference preference, boolean status){
        if (status)
            preference.setSummary("Notifications enabled");
        else
            preference.setSummary("Notifications disabled");
    }

    private final void updateNotificationTime(Preference preference, long time){
        /*Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(time));
        int hour = cal.get(Calendar.HOUR);
        int minute = cal.get(Calendar.MINUTE);
        int ampm = cal.get(Calendar.AM_PM);
        String am_pm = null;
        if (ampm == Calendar.AM)
            am_pm = "AM";
        else
            am_pm = "PM";
            */
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("h:mm a");
        //String notificationTime = Integer.toString(hour) + ":" + Integer.toString(minute) + " " + am_pm;
        String notificationTime = simpleDateFormat.format(new Date(time));
        preference.setSummary(notificationTime);

        // Send event to GA
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Settings")
                .setAction("Change")
                .setLabel("Notification_time")
                .setValue(1)
                .build());
    }

    private final void updateJournalTime(Preference preference, long time){
        /*Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(time));
        int hour = cal.get(Calendar.HOUR);
        int minute = cal.get(Calendar.MINUTE);
        int ampm = cal.get(Calendar.AM_PM);
        String am_pm = null;
        if (ampm == Calendar.AM)
            am_pm = "AM";
        else
            am_pm = "PM";
        String notificationTime = Integer.toString(hour) + ":" + Integer.toString(minute) + " " + am_pm;*/
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("h:mm a");
        String notificationTime = simpleDateFormat.format(new Date(time));
        preference.setSummary(notificationTime);

        // Send event to GA
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("Settings")
                .setAction("Change")
                .setLabel("Journal_time")
                .setValue(1)
                .build());
    }

    @Override
    public void onCreateOptionsMenu(
            Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_settings, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
