package com.raman.myapplication4;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by RUPANSHU on 08-Apr-17.
 */

public class AppStarter extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        //this will send a notification message
        Log.i("AS", "AS receiver called");
        Log.i("AS", "intent got=" + intent.getAction());
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction()) ||
                "android.intent.action.MY_PACKAGE_REPLACED".equals(intent.getAction())) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedPref.getBoolean(context.getString(R.string.key_scheduler_status), false)) {
                Log.i("AS", "UC started by boot receiver...");
                long[] timeFrameSize = new long[]{300000, 600000, AlarmManager.INTERVAL_FIFTEEN_MINUTES, 1200000, AlarmManager.INTERVAL_HALF_HOUR};
                long interval = timeFrameSize[Integer.parseInt(sharedPref.getString(context.getString(R.string.key_window_size), "2"))];
                setAlarm(context, sharedPref, interval);
                if (sharedPref.getBoolean(context.getString(R.string.key_check_fol_for_half_day), false))
                    setNewAlarmForHalfDay(context, interval, sharedPref);
            }
        }
        Log.i("AS", "UC Started");
    }

    private void setAlarm(Context context, SharedPreferences sharedPref, long interval) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar mcurrentTime = Calendar.getInstance();
        mcurrentTime.set(Calendar.HOUR_OF_DAY, sharedPref.getInt(context.getString(R.string.key_selected_hour), 8));
        mcurrentTime.set(Calendar.MINUTE, sharedPref.getInt(context.getString(R.string.key_selected_min), 0));
        final Intent myIntent = new Intent(context, Notify.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, myIntent, 0);
        if (Build.VERSION.SDK_INT >= 19) {
            if (Calendar.getInstance().getTimeInMillis() - mcurrentTime.getTimeInMillis() < 0) {
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP,
                        mcurrentTime.getTimeInMillis() - interval, interval, pendingIntent);
                Log.i("AS", "Alarm set for today at " + mcurrentTime.getTime());
            } else {
                //past time set for next day
                Log.i("AS", "Alarm set for tomorrow at " + mcurrentTime.getTime());
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, mcurrentTime.getTimeInMillis() - interval + AlarmManager.INTERVAL_DAY, interval, pendingIntent);
            }
        } else {
            //For older versions setRepeating alarms are exact
            if (Calendar.getInstance().getTimeInMillis() < mcurrentTime.getTimeInMillis()) {
                Log.i("AS", "Reminder set by 4.4 at start");
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, mcurrentTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
            }
        }
        Intent startServiceIntent = new Intent(context, NotificationService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            Log.d("AS", "YEPP DoN't Crash");
            context.startForegroundService(startServiceIntent);
        } else
        context.startService(startServiceIntent);
    }

    public void setNewAlarmForHalfDay(Context context, long interval, SharedPreferences sharedPref) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar mcurrentTime = Calendar.getInstance();
        mcurrentTime.set(Calendar.HOUR_OF_DAY, 12);
        final Intent myIntent = new Intent(context, Notify.class);
        myIntent.putExtra(context.getString(R.string.requestCode), "2");
        mcurrentTime.set(Calendar.MINUTE, 0);
        int[] addDelayArray = new int[]{0, 15, 30, 45, 60, 90, 120, 180};
        int addDelay = addDelayArray[Integer.parseInt(sharedPref.getString(context.getString(R.string.key_delay_for_half_day_check), "1"))];
        mcurrentTime.add(Calendar.MINUTE, addDelay);

        Log.i("MA", "half day time is " + mcurrentTime.getTime());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 2, myIntent, 0);
        if (Build.VERSION.SDK_INT >= 19) {
            if (Calendar.getInstance().getTimeInMillis() - mcurrentTime.getTimeInMillis() < 0) {
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP,
                        mcurrentTime.getTimeInMillis() - interval, interval, pendingIntent);
                Log.i("AS", "Half day Alarm set for today at " + mcurrentTime.getTime());
            } else {
                //past time set for next day
                Log.i("AS", "Half day Alarm set for tomorrow at " + mcurrentTime.getTime());
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, mcurrentTime.getTimeInMillis() - interval + AlarmManager.INTERVAL_DAY, interval, pendingIntent);
            }
        } else {
            //For older versions setRepeating alarms are exact
            if (Calendar.getInstance().getTimeInMillis() < mcurrentTime.getTimeInMillis()) {
                Log.i("AS", "Half day Reminder set by 4.4 at start");
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, mcurrentTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
            }
        }
    }
}