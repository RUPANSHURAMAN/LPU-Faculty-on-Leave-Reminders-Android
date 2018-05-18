package com.raman.myapplication4;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by RUPANSHU on 08-Apr-17.
 */

public class NotificationSender extends IntentService {
    StringBuilder sb;
    int count;
    SharedPreferences sharedPref;
    private List<String> courseCode, teacherName, daySpan;
    private StringBuilder shareText;

    public NotificationSender() {
        super("NotificationSender");
    }

    @Override
    public void onHandleIntent(final Intent intent) {
        sb = new StringBuilder();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean turnWifiOff = isWiFiOff()
                && sharedPref.getBoolean(getString(R.string.key_turn_wifi_off), true)
                || !sharedPref.getBoolean(getString(R.string.key_downloading_prevention), true);
        Log.i("NSender", "TURN WIFI OFF :" + turnWifiOff);
        if (!isConnected(getApplicationContext()))
            toggleWiFi(true);
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    //checks if screen off then don't send fake
                    if (isScreenOff() && isWiFiOff()) {
                        Intent intent1 = new Intent(getApplicationContext(), Fake.class);
                        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent1);
                    }
                    int timeout = 0;
                    //check if connected
                    while (!isConnected(getApplicationContext())) {
                        //Wait to connect
                        int[] timeoutArray = new int[]{15, 30, 45, 60};
                        if (timeout >= timeoutArray[Integer.parseInt(sharedPref.getString(getString(R.string.key_time_to_wait_for_networks), "1"))]) {
                            Log.i("NSender", "No networks were found... TIMEOUT");
                            sendNotification(getString(R.string.no_wifi), -2);
                            toggleWiFi(false);
                            return;
                        }
                        timeout++;
                        Thread.sleep(1000);
                        Log.i("NSender", "tick tock " + timeout);
                    }
                    int[] waitForActiveConnectionArray = new int[]{3, 5, 10, 15, 20};
                    int waitForActiveConnection = waitForActiveConnectionArray[Integer.parseInt(sharedPref.getString(getString(R.string.key_time_to_wait_for_active_connection), "0"))];
                    //add 3 or desired extra sec for establishing active connection if not already connected
                    if (timeout != 0) {
                        Log.i("NSender", "added " + waitForActiveConnection + " sec to establish active connection");
                        while (waitForActiveConnection-- > 0)
                            Thread.sleep(1000);
                    }
                    int[] maxRetriesArray = new int[]{3, 5, 7, 10};
                    for (int i = 0; i < maxRetriesArray[Integer.parseInt(sharedPref.getString(getString(R.string.key_select_max_retries), "1"))]; i++) {
                        if (checkFolService(sharedPref.getString(getString(R.string.key_registration_number), "a"),
                                sharedPref.getString(getString(R.string.key_password), "a")))
                            break;
                        Thread.sleep(1000);
                        Log.i("NSender", "Sleep 1 sec before retry :" + (i + 2));
                    }
                    sendNotification(sb.toString(), count);
                    if (turnWifiOff)
                        toggleWiFi(false);
                    Notify.completeWakefulIntent(intent);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    public boolean isConnected(Context context) {
        NetworkInfo networkInfo = null;
        if (context.getSystemService(Context.CONNECTIVITY_SERVICE) != null) {
            networkInfo = ((ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        }
        Log.i("NSender", "isConnected" + (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED));
        return networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED;
    }

    public void toggleWiFi(boolean status) {
        final WifiManager wifiManager = (WifiManager) this
                .getSystemService(Context.WIFI_SERVICE);
        if (status && !wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        } else if (!status && wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
        wifiManager.reconnect();
        Log.i("WIFI-NSender", "Wifi is " + (wifiManager.isWifiEnabled() ? "ON" : "OFF"));
    }

    private void sendNotification(String msg, int count) {
        Log.i("AlarmService", "Preparing to send notification...: " + msg + " & count=" + count);
        NotificationManager alarmNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        Notification notif;
        String contenttitle = "Faculty on Leave", contenttext;
        if (count == -1)
            contenttext = sb.toString();
        else if (count == -2)
            contenttext = msg;
        else
            contenttext = count + " " + (count == 1 ? getString(R.string.prefix_no_of_faculty_single).toLowerCase() : getString(R.string.prefix_no_of_faculty)).toLowerCase();
        if (sb.toString().isEmpty())
            sb = new StringBuilder(contenttext);
        if (sb.toString().equals(getString(R.string.password_changed_tap_msg_or_update_needed)))
            contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, LoginActivity.class), 0);
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        String extraText = "*Faculty on Leave*\n--------------------------\n" + shareText + "\n*Automatically checked by LPU UnderCover app*\n" + getString(R.string.play_store_link);
        shareIntent.putExtra(Intent.EXTRA_TEXT, extraText);
        shareIntent.setType("text/plain");
        PendingIntent pendingShareIntent = PendingIntent.getActivity(this, 1, Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)),
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pendingSettingsIntent = PendingIntent.getActivity(this, 2, new Intent(this, SettingsActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel mChannel = new NotificationChannel(getString(R.string.app_name), getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH);
            mChannel.setDescription(getString(R.string.description_fol_reminder));
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            alarmNotificationManager.createNotificationChannel(mChannel);
            notif = new Notification.Builder(getApplicationContext())
                    .setContentTitle(contenttitle)
                    .setContentText(contenttext)
                    .setChannelId(getString(R.string.app_name))
                    .addAction(R.drawable.ic_share_black_24dp, getString(R.string.share), pendingShareIntent)
                    .addAction(R.drawable.ic_settings_black_24px, getString(R.string.settings), pendingSettingsIntent)
                    .setSmallIcon(R.drawable.ic_notif)
                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.FLAG_FOREGROUND_SERVICE)
                    .setStyle(new Notification.BigTextStyle().bigText(sb))
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)
                    .build();
        } else if (Build.VERSION.SDK_INT >= 16) {
            notif = new Notification.Builder(getApplicationContext())
                    .setContentTitle(contenttitle)
                    .setContentText(contenttext)
                    .addAction(R.drawable.ic_share_black_24dp, getString(R.string.share), pendingShareIntent)
                    .addAction(R.drawable.ic_settings_black_24px, getString(R.string.settings), pendingSettingsIntent)
                    .setSmallIcon(R.drawable.ic_notif)
                    .setDefaults(Notification.DEFAULT_SOUND | Notification.FLAG_SHOW_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.FLAG_FOREGROUND_SERVICE)
                    .setStyle(new Notification.BigTextStyle().bigText(sb))
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)
                    .setPriority(Notification.PRIORITY_MAX)
                    .build();
        } else {//Not required
            notif = new Notification.Builder(getApplicationContext())
                    .setContentTitle(contenttitle).setContentText(sb)
                    .addAction(R.drawable.ic_share_black_24dp, getString(R.string.share), pendingShareIntent)
                    .addAction(R.drawable.ic_settings_white_24dp, getString(R.string.settings), pendingSettingsIntent)
                    .setSmallIcon(R.drawable.ic_notif).getNotification();
        }

        Log.i("NSender", "Notification Sent");
        alarmNotificationManager.notify(100, notif);
    }

    private boolean checkFolService(String mRegistrationNumber, String mPassword) {
        try {
            count = -1;
            sb.setLength(0);
            Log.i("NSender", "Checking");
            // Simulate network access.
            Connection.Response response = Jsoup.connect(getString(R.string.ums_login_url))
                    .method(Connection.Method.GET)
                    .execute();
            String viewstate = response.parse().select("input[name=__VIEWSTATE]").first().attr("value");
            if (response.statusCode() == 302 || response.parse().text().contains(getString(R.string.oops_ums_server_down_error)) || viewstate == null) {
                sb.append(getString(R.string.ums_server_busy));
                return false;
            }
            response =
                    Jsoup.connect(getString(R.string.ums_login_url))
                            .data(getString(R.string.__LASTFOCUS), getString(R.string.__LASTFOCUS_value))
                            .data(getString(R.string.__EVENTTARGET), getString(R.string.__EVENTTARGET_value))
                            .data(getString(R.string.__EVENTARGUMENT), getString(R.string.__EVENTARGUMENT_value))
                            .data(getString(R.string.__VIEWSTATE), viewstate)
                            .data(getString(R.string.__VIEWSTATEGENERATOR), getString(R.string.__VIEWSTATEGENERATOR_value))
                            .data(getString(R.string.__SCROLLPOSITIONX), getString(R.string.__SCROLLPOSITIONX_value))
                            .data(getString(R.string.__SCROLLPOSITIONY), getString(R.string.__SCROLLPOSITIONY_value))
                            .data(getString(R.string.__VIEWSTATEENCRYPTED), getString(R.string.__VIEWSTATEENCRYPTED_value))
                            .data(getString(R.string.__USERNAME), mRegistrationNumber)
                            .data(getString(R.string.__PASSWORD), mPassword)
                            .data(getString(R.string.__DropDownList1), getString(R.string.__DropDownList1_value))
                            .data(getString(R.string.__ddlStartWith), getString(R.string.__ddlStartWith_value))
                            .data(getString(R.string.__iBtnLogin_x), getString(R.string.__iBtnLogin_x_value))
                            .data(getString(R.string.__iBtnLogin_y), getString(R.string.__iBtnLogin_y_value))
                            .cookies(response.cookies())
                            .method(Connection.Method.POST)
                            .followRedirects(true)
                            .execute();
            if (response.parse().text().contains(getString(R.string.oops_ums_server_down_error))) {
                sb.append(getString(R.string.try_again));
                return false;
            }
            if (response.parse().title().equals(getString(R.string.ums_login_page_title))) {
                sb.append(getString(R.string.password_changed_tap_msg_or_update_needed));
                return false;
            }
            shareText = new StringBuilder();
            Document document = response.parse();
            if (document.select(getString(R.string.ums_fol_table_id)).text().equals(getString(R.string.no_faculty_on_leave))) {
                count = -1;
                sb.append(getString(R.string.no_faculty_on_leave));
                shareText.append(getString(R.string.no_faculty_on_leave));
                saveResultInPrefs(shareText, sharedPref);
                return true;
            }
            Element table = document.select(getString(R.string.ums_fol_table_id)).last();
            Elements tds;
            courseCode = new ArrayList<>();
            teacherName = new ArrayList<>();
            daySpan = new ArrayList<>();
            for (Element row : table.select("tr")) {
                tds = row.select("td");
                if (tds.size() > 0) {
                    courseCode.add(tds.get(0).text());
                    teacherName.add(tds.get(2).text());
                    daySpan.add(tds.get(3).text());
                    shareText.append("\u25CF").append(tds.get(2).text()).append("\t").append(tds.get(3).text()).append("\n");
                    sb.append(tds.get(0).text()).append(" - ").append(tds.get(2).text()).append(" - ").append(tds.get(3).text()).append("\n");
                }
            }
            saveResultInPrefs(shareText, sharedPref);
            count = courseCode.size();
            //log out
            Jsoup.connect(getString(R.string.ums_login_url))
                    .method(Connection.Method.GET)
                    .execute();
        } catch (SocketTimeoutException timeout) {
            int[] maxRetriesArray = new int[]{3, 5, 7, 10};
            sb.append(getString(R.string.connection_timed_out)).append(" after ")
                    .append(maxRetriesArray[Integer.parseInt(sharedPref.getString(getString(R.string.key_select_max_retries), "1"))])
                    .append(" retries");
            timeout.printStackTrace();
            return false;
        } catch (UnknownHostException uhe) {
            sb.append(getString(R.string.no_wifi));
            uhe.printStackTrace();
            return false;
        } catch (IOException e) {
            sb.append(getString(R.string.network_error));
            e.printStackTrace();
            return false;
        } catch (NullPointerException npe) {
            sb.append(getString(R.string.try_again));
            npe.printStackTrace();
            return false;
        }
        return true;
    }

    private void saveResultInPrefs(StringBuilder shareText, SharedPreferences sharedPref) {
        //save for scheduler
        sharedPref.edit().putLong(getString(R.string.key_time_stamp_scheduler), System.currentTimeMillis()).apply();
        new Util().saveFoLResult(sharedPref, courseCode, teacherName, daySpan, shareText.toString());
    }

    public boolean isScreenOff() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return Build.VERSION.SDK_INT > 19 ? !pm.isInteractive() : !pm.isScreenOn();
    }

    public boolean isWiFiOff() {
        WifiManager wifiManager = (WifiManager) this
                .getSystemService(Context.WIFI_SERVICE);
        return !wifiManager.isWifiEnabled();
    }
}