package com.antwish.povi.familyconnect;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import android.support.multidex.MultiDexApplication;

import java.util.HashMap;

/**
 * An extension to Application class to provide tracker for analytics purposes. Having the tracker
 * instances here allows all the activities to access the same tracker instances. The trackers can
 * be initialised on startup or when they are required based on performance requirements.
 */
public class AnalyticsPoviApp extends MultiDexApplication {

    // The following line should be changed to include the correct property id.
    //private static final String PROPERTY_ID = "UA-64554804-1";
    private static final String PROPERTY_ID = "UA-68865824-1";

    /**
     * Enum used to identify the tracker that needs to be used for tracking.
     *
     * A single tracker is usually enough for most purposes. In case you do need multiple trackers,
     * storing them all in Application object helps ensure that they are created only once per
     * application instance.
     */
    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        //ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    public AnalyticsPoviApp() {
        super();
    }
    synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // Set analytics
            analytics.getLogger().setLogLevel(com.google.android.gms.analytics.Logger.LogLevel.VERBOSE);
            analytics.setDryRun(false);
            analytics.setLocalDispatchPeriod(30);

            Tracker t = (trackerId == TrackerName.APP_TRACKER) ? analytics.newTracker(PROPERTY_ID)
                    :analytics.newTracker(PROPERTY_ID);
                    //: (trackerId == TrackerName.GLOBAL_TRACKER) ? analytics.newTracker(R.xml.analytics_global_config)
                   // : analytics.newTracker(R.xml.ecommerce_tracker);
            // Set tracker manually
            t.enableAdvertisingIdCollection(true);
            t.enableAutoActivityTracking(false);
            t.enableExceptionReporting(true);// TODO: roll-back to true when deploying!
            t.setAnonymizeIp(false);
            t.setSampleRate(100.0);
            t.setSessionTimeout(1800);
            t.setAppName("Povi Family Connect");

            mTrackers.put(trackerId, t);

        }
        return mTrackers.get(trackerId);
    }
}
