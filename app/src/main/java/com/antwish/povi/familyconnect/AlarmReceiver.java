package com.antwish.povi.familyconnect;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.antwish.povi.server.ParentingTip;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.linkedin.data.template.GetMode;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    private static final String TIP_OF_THE_DAY = "Tip Of The Day";
    private static final String JOURNAL_TIME = "It's time to write your journal!";
    private static final String JOURNAL_REMINDER = "Povi Journal Reminder";
    private static int mId = 1;
    private static final int maxTipLength = 30;

    private static final String POVI_TOKEN = "povi_token";
    private static final String POVI_USERID = "povi_userid";

    private Tracker tracker;

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.e(TAG, "onReceive called");

        AlarmUtils.NotificationType notificationType = AlarmUtils.NotificationType.valueOf(intent.getStringExtra(AlarmUtils.NOTIFICATION_TYPE));
        sendNotification(context, notificationType);
    }

    private void sendNotification(Context context, AlarmUtils.NotificationType notificationType) {
        // Get tracker and signals the user entered the registration screen
        tracker = ((AnalyticsPoviApp) context.getApplicationContext()).getTracker(
                AnalyticsPoviApp.TrackerName.GLOBAL_TRACKER);
        tracker.setScreenName("Notification screen");

        String contentTitle, contextText, tip=null;
        Intent resultIntent = null;
        switch (notificationType)
        {
            case JOURNALTIME:
                contentTitle = JOURNAL_REMINDER;
                contextText = JOURNAL_TIME;
                tip = getTip(context);
                resultIntent = new Intent(context, DashboardActivity.class);
                //resultIntent.putExtra(TipjarFragment.TIP, tip);
                //resultIntent.putExtra(TipjarFragment.FROM_JOURNAL_REMINDER, true);
                // Send event to GA
                tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Notification")
                        .setAction("Received")
                        .setLabel("Journal_reminder")
                        .build());
                break;
            case TIPTIME:
                contentTitle = TIP_OF_THE_DAY;
                tip = getTip(context);
                contextText = tip.substring(0, tip.length() > maxTipLength ? maxTipLength : tip.length());
                resultIntent = new Intent(context, DashboardActivity.class);
                //resultIntent.putExtra(TipjarFragment.TIP, tip);
                //resultIntent.putExtra(TipjarFragment.FROM_TIP_REMINDER, true);
                // Send event to GA
                tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Notification")
                        .setAction("Received")
                        .setLabel("Tip_remainder")
                        .build());
                break;
            default:
                return;
        }

        Notification.Builder mBuilder =
                new Notification.Builder(context)
                        .setSmallIcon(R.drawable.povi_beta_icon)
                        .setContentTitle(contentTitle)
                        .setContentText(contextText)
                        .setAutoCancel(true);
        // Creates an explicit intent for an Activity in your app

        Log.e(TAG, "will create a notification");

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        stackBuilder.addParentStack(DashboardActivity.class);

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(mId++, mBuilder.build());
    }

    private String getTip(Context context)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String token = sharedPref.getString(POVI_TOKEN, null);
        String userId = sharedPref.getString(POVI_USERID, null);

        int i = 0;
        while(i < 5)
        {
            Date date = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            String currentDate = sdf.format(cal.getTime());
            ParentingTip[] tips = RestServer.getTips(token, userId, currentDate);
            if(tips != null && tips.length > 0) return tips[0].getTipDetail(GetMode.NULL);
            i++;
        }

        return "";
    }
}
