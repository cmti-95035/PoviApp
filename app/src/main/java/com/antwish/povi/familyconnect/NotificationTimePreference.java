package com.antwish.povi.familyconnect;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Fabio Dominio on 14/05/2015.
 */
public class NotificationTimePreference extends DialogPreference {
    private TimePicker timePicker;
    private Context context;
    private String key;
    private static final String AM = "AM";
    private static final String PM = "PM";
    public NotificationTimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false);
        setDialogLayoutResource(R.layout.time_reference);
        this.context = context;
    }

    @Override
    public View onCreateDialogView(){
        timePicker =  new TimePicker(context);
        timePicker.setIs24HourView(false);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        Date date = new Date(settings.getLong("notification_time",0));
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        timePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
        timePicker.setCurrentMinute(cal.get(Calendar.MINUTE));
        return timePicker;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult){
        super.onDialogClosed(positiveResult);
        if(positiveResult){
            Calendar cal = Calendar.getInstance();
            cal.set(0,0,0, timePicker.getCurrentHour(), timePicker.getCurrentMinute());
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putLong("notification_time", cal.getTime().getTime());
            editor.commit();
            callChangeListener(cal.getTime().getTime());

            // set the alarm manager
            AlarmUtils.scheduleAlarm(
                    context,
                    AlarmUtils.calculateTimeDifference(timePicker.getCurrentHour(), timePicker.getCurrentMinute()),
                    AlarmUtils.NotificationType.TIPTIME
            );
        }
    }
}
