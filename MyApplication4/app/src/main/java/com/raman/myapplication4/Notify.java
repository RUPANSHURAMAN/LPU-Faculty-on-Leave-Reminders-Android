package com.raman.myapplication4;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.text.format.DateFormat;
import android.util.Log;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by RUPANSHU on 08-Apr-17.
 *
 */
//TODO Imp wake deprecated
public class Notify extends WakefulBroadcastReceiver {

    @Override
    //this will send a notification message
    public void onReceive(final Context context, Intent intent) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String schedulerWeek = sharedPref.getString(context.getString(R.string.key_selected_days), Arrays.toString(new int[]{0, 1, 2, 3, 4}));
        if (intent.getExtras() != null)
            if (intent.getExtras().getString(context.getString(R.string.requestCode), "-1").equals("2")) {
                sharedPref.edit().putString(context.getString(R.string.key_today_done_date_half_day), (String) DateFormat.format("dd", new Date())).apply();
                Log.i("Notify", "half day value changed");
            }
        //If today its meant to be then do it
        Log.i("Notify", "Received alarm service for reminder notif");
        if (schedulerWeek.contains(String.valueOf(new Util().getDayAccToSaved(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))))) {
            Log.i("Notify", "Sending Notification after week of day check result true");
            Intent service = new Intent(context, NotificationSender.class);
            startWakefulService(context, service);
        }
        long[] timeFrameSize = new long[]{300000, 600000, AlarmManager.INTERVAL_FIFTEEN_MINUTES, 1200000, AlarmManager.INTERVAL_HALF_HOUR};
        long interval = timeFrameSize[Integer.parseInt(sharedPref.getString(context.getString(R.string.key_window_size), "2"))];
        setNewAlarm(context, sharedPref, interval);
        if (sharedPref.getBoolean(context.getString(R.string.key_check_fol_for_half_day), false) && new Util().isHalfDayTodayDone(context, sharedPref)) {
            setNewAlarmForHalfDay(context, interval, sharedPref);
        }
    }

    private void setNewAlarmForHalfDay(Context context, long interval, SharedPreferences sharedPref) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar mcurrentTime = Calendar.getInstance();
        mcurrentTime.set(Calendar.HOUR_OF_DAY, 12);
        mcurrentTime.set(Calendar.MINUTE, 0);
        int[] addDelayArray = new int[]{0, 15, 30, 45, 60, 90, 120, 180};
        int addDelay = addDelayArray[Integer.parseInt(sharedPref.getString(context.getString(R.string.key_delay_for_half_day_check), "1"))];
        mcurrentTime.add(Calendar.MINUTE, addDelay);

        Log.i("MA", "half day time is " + mcurrentTime.getTime());
        final Intent myIntent = new Intent(context, Notify.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 2, myIntent, 0);
        alarmManager.cancel(pendingIntent);
        if (Build.VERSION.SDK_INT >= 19) {
            /*if (Calendar.getInstance().getTimeInMillis() < mcurrentTime.getTimeInMillis()
                    && !new Util().isHalfDayTodayDone(context, sharedPref)
                    && sharedPref.getString(context.getString(R.string.key_selected_days),"01234").contains(String.valueOf(new Util().getDayAccToSaved(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))))) {
                sharedPref.edit().putString(context.getString(R.string.key_today_done_date_half_day), (String) DateFormat.format("dd", new Date())).apply();
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, mcurrentTime.getTimeInMillis() - interval, interval, pendingIntent);
                Log.i("Notify", "Half day Alarm set for today at " + mcurrentTime.getTime());
            } else {*/
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, mcurrentTime.getTimeInMillis() - interval + AlarmManager.INTERVAL_DAY, interval, pendingIntent);
                Log.i("Notify", "Half day Alarm set for tomorrow at " + mcurrentTime.getTime());
        }
    }

    private void setNewAlarm(Context context, SharedPreferences sharedPref, long interval) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar mcurrentTime = Calendar.getInstance();
        mcurrentTime.set(Calendar.HOUR_OF_DAY, sharedPref.getInt(context.getString(R.string.key_selected_hour), 8));
        mcurrentTime.set(Calendar.MINUTE, sharedPref.getInt(context.getString(R.string.key_selected_min), 0));
        Log.i("TEMP1", "Time is " + mcurrentTime.get(Calendar.HOUR_OF_DAY) + ":" + mcurrentTime.get(Calendar.MINUTE));
        final Intent myIntent = new Intent(context, Notify.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, myIntent, 0);
        alarmManager.cancel(pendingIntent);
        sharedPref.edit().putString(context.getString(R.string.key_today_done_date), (String) DateFormat.format("dd", new Date())).apply();
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, mcurrentTime.getTimeInMillis() - interval + AlarmManager.INTERVAL_DAY, interval, pendingIntent);
            Log.i("Notify", "Alarm set for tomorrow at " + mcurrentTime.getTime());
        }
    }
}