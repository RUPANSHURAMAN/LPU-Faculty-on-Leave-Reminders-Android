package com.raman.myapplication4;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by RUPANSHU on 29-May-17.
 */

public class NotificationService extends Service {
    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("NServiceReceiver", "Broadcast received");
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            //improve
            //if (sharedPref.getInt(getString(R.string.key_scheduler_status), -1) != -1) {
            if (sharedPref.getInt(getString(R.string.key_selected_hour), -1) != -1) {
                startService(new Intent(context, NotificationService.class));
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("NService", "onStartCmd called");
        addNotification();
        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(myReceiver, screenStateFilter);
        return START_STICKY;
    }

    private void addNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        Util util = new Util();
        String contenttext = getString(R.string.next_up) + " "
                + util.getRelativeTimeString(getApplicationContext(), sharedPref);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel mChannel = new NotificationChannel("service", getString(R.string.channel_name_service), NotificationManager.IMPORTANCE_NONE);
            mChannel.setDescription(getString(R.string.description_fol_reminder_service));
            notificationManager.createNotificationChannel(mChannel);
            notification = new Notification.Builder(getApplicationContext())
                    .setContentTitle("Reminders "
                            + util.convertToChoosenHourFormat(sharedPref.getInt(getString(R.string.key_selected_hour), 8) + ":"
                            + sharedPref.getInt(getString(R.string.key_selected_min), 0), sharedPref))
                    .setChannelId(getString(R.string.channel_name_service))
                    .setContentText(contenttext)
                    .setChannelId(getString(R.string.app_name))
                    .setSmallIcon(R.drawable.ic_notif)
                    .setContentIntent(pendingIntent)
                    .build();
            /*notificationManager.notify(2,notification);
            return;*/
        } else if (Build.VERSION.SDK_INT >= 16) {
            notification = new Notification.Builder(this)
                    .setContentTitle("Reminders "
                            + util.convertToChoosenHourFormat(sharedPref.getInt(getString(R.string.key_selected_hour), 8) + ":"
                            + sharedPref.getInt(getString(R.string.key_selected_min), 0), sharedPref))
                    .setSmallIcon(R.drawable.ic_notif)
                    .setContentText(contenttext)
                    .setContentIntent(pendingIntent)
                    .setTicker("Reminer service ON")
                    .setPriority(Notification.PRIORITY_MIN)
                    .build();
        } else {
            notification = new Notification.Builder(getApplicationContext())
                    .setContentTitle(getString(R.string.app_name)).setContentText("Service for FoL Reminder")
                    .setSmallIcon(R.drawable.ic_cancel).getNotification();
        }
        startForeground(2, notification);
        Log.i("NService", "notification added");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("NService", "Service also launched");
        return null;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(myReceiver);
        Toast.makeText(this, "Reminder service stopped", Toast.LENGTH_SHORT).show();
    }
}
