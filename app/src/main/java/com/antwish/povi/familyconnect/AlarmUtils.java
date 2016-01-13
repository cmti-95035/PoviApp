package com.antwish.povi.familyconnect;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class AlarmUtils {

    private static final long MILLISECONDS_A_DAY = 24*60*60*1000L;
    public static enum NotificationType {JOURNALTIME, TIPTIME};
    public static enum AlarmType {JOURNAL, TIP};
    public static final String NOTIFICATION_TYPE = "notification_type";

    /**
     * calculate the time difference between current time to a preset hour and minute in millisecond
     * if the current time is later than the preset time then return the difference of the current time
     * to the preset time of the next day
     *
     * @param alarmHour
     * @param alarmMinute
     * @return
     */
    public static long calculateTimeDifference(int alarmHour, int alarmMinute)
    {
        Calendar cal = Calendar.getInstance();
        int totalMinutes = cal.getTime().getHours() * 60 + cal.getTime().getMinutes();
        int alarmMinutes = alarmHour * 60 + alarmMinute;

        long offset = (alarmMinutes - totalMinutes) * 60 * 1000l;
        if(totalMinutes < alarmMinutes )
            return offset;  // alarm is set later of the day
        else
            return offset + MILLISECONDS_A_DAY; // alarm is set on the next day
    }

    /**
     * schedule the alarm per the context
     * @param context
     * @param offset
     * @param notificationType
     */
    public static void scheduleAlarm(Context context, long offset, NotificationType notificationType )
    {

        Long time = new GregorianCalendar().getTimeInMillis() + offset;
        // create an Intent and set the class which will execute when Alarm triggers, here we have
        // given AlarmReciever in the Intent, the onRecieve() method of this class will execute when
        // alarm triggers and
        //we will write the code to send SMS inside onRecieve() method pf Alarmreciever class
        Intent intentAlarm = new Intent(context, AlarmReceiver.class);

        intentAlarm.putExtra(NOTIFICATION_TYPE, notificationType.name());

        final int alarmId = notificationType == NotificationType.JOURNALTIME ? AlarmType.JOURNAL.ordinal() : AlarmType.TIP.ordinal();
        // find any previous scheduled alarm and cancel them
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarmId, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);

        // create the object
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if(pendingIntent != null) {
            pendingIntent.cancel();
            alarmManager.cancel(pendingIntent);
        }

        //set the alarm for particular time
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, time, MILLISECONDS_A_DAY, PendingIntent.getBroadcast(context, alarmId, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
    }
}
