package com.raman.myapplication4;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.MenuItem;

import java.util.Calendar;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    static String storedValueHalfDayDelay, storedValue;
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        void setHalfDayReminder(Context context, String stringValue) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            if (!sharedPref.getBoolean(context.getString(R.string.key_scheduler_status), false))
                return;
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            final Intent myIntent = new Intent(context, Notify.class);
            myIntent.putExtra(context.getString(R.string.requestCode), "2");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 2, myIntent, 0);
            alarmManager.cancel(pendingIntent);
            Calendar mcurrentTime = Calendar.getInstance();
            //TODO add jitter
            mcurrentTime.set(Calendar.HOUR_OF_DAY, 12);
            mcurrentTime.set(Calendar.MINUTE, 0);
            int[] addDelayArray = new int[]{0, 15, 30, 45, 60, 90, 120, 180};
            int addDelay = addDelayArray[Integer.parseInt(stringValue)];
            mcurrentTime.add(Calendar.MINUTE, addDelay);

            Log.i("MA", "half day time is " + mcurrentTime.getTime());
            long[] timeFrameSize = new long[]{300000, 600000, AlarmManager.INTERVAL_FIFTEEN_MINUTES, 1200000, AlarmManager.INTERVAL_HALF_HOUR};
            long interval = timeFrameSize[Integer.parseInt(sharedPref.getString(context.getString(R.string.key_window_size), "2"))];
            //sharedPref.edit().putString(context.getString(R.string.key_today_done_date_half_day), "0").apply();
            if (Build.VERSION.SDK_INT >= 19) {
                if (Calendar.getInstance().getTimeInMillis() - mcurrentTime.getTimeInMillis() < 0
                        && !new Util().isHalfDayTodayDone(context, sharedPref)
                        && sharedPref.getString(context.getString(R.string.key_selected_days), "01234").contains(String.valueOf(new Util().getDayAccToSaved(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))))) {
                    alarmManager.setWindow(AlarmManager.RTC_WAKEUP, mcurrentTime.getTimeInMillis() - interval, interval, pendingIntent);
                    Log.i("SA", "Today set Reminder");
                } else {
                    //past time set for next day
                    Log.i("SA", "one day later set Reminder");
                    alarmManager.setWindow(AlarmManager.RTC_WAKEUP, mcurrentTime.getTimeInMillis() - interval + AlarmManager.INTERVAL_DAY, interval, pendingIntent);
                }
            } else {
                //For older versions setRepeating alarms are exact
                Log.i("SA", "set repeating for lower than 4.4 android");
                if (Calendar.getInstance().getTimeInMillis() + 60000 < mcurrentTime.getTimeInMillis()) {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, mcurrentTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
                    Log.i("SA", "Today set Reminder for 4.4");
                } else {
                    Log.i("SA", "one day later set Reminder for 4.4");
                }
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            Log.d("SA", "stringValue got: " + value);
            String stringValue = value.toString();

            if (preference instanceof SwitchPreference) {
                if (preference.getKey().equals("key_is_24_hour_format")) {
                    if (stringValue.equals("true"))
                        preference.setSummary("13:00");
                    else preference.setSummary("1:00 PM");
                } else if (preference.getKey().equals("key_check_fol_for_half_day")) {
                    if (stringValue.equals("true")) {
                        Log.i("SA", "storedValue :" + storedValue);
                        if (!storedValue.equals(stringValue)) {
                            PreferenceManager.getDefaultSharedPreferences(preference.getContext()).edit()
                                    .putString(preference.getContext().getString(R.string.key_today_done_date_half_day), "0").apply();
                            setHalfDayReminder(preference.getContext(), "1");
                        } else Log.i("SA", "Not CALL to set reminder");
                        preference.setSummary("Will be delivered at below chosen time");
                    } else {
                        cancelHalfDayReminder(preference.getContext());
                        preference.setSummary(preference.getContext().getString(R.string.summary_check_fol_for_half_day));
                    }
                }
            } else if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                if (preference.getKey().equals(preference.getContext().getString(R.string.key_delay_for_half_day_check))) {
                    if (!storedValueHalfDayDelay.equals(stringValue)) {
                        PreferenceManager.getDefaultSharedPreferences(preference.getContext()).edit()
                                .putString(preference.getContext().getString(R.string.key_today_done_date_half_day), "0").apply();
                        setHalfDayReminder(preference.getContext(), stringValue);
                    } else Log.i("SA", "Not calling to set reminder");
                }
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } /*else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            }*/ else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }

        private void cancelHalfDayReminder(Context context) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            final Intent myIntent = new Intent(context, Notify.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 2, myIntent, 0);
            alarmManager.cancel(pendingIntent);
            Log.i("SA", "Half day Reminder alarm cancelled");
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        if (preference instanceof SwitchPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), false));
            return;
        }
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * Email client intent to send support mail
     * Appends the necessary device information to email body
     * useful when providing support
     */
    /*public static void showTimePicker(Context context){
        Preference preference = getPreferenceManager().findPreference("timePrefA_Key");
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d("Preference ", "" + preference);
                Log.d("newValue", "" + newValue);
                return true;
            }
        });
    }*/
    public static void sendFeedback(Context context) {
        String body = null;
        try {
            body = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            body = "\n\n-----------------------------\nPlease don't remove this information\n Device OS: Android \n Device OS version: " +
                    Build.VERSION.RELEASE + "\n App Version: " + body + "\n Device Brand: " + Build.BRAND +
                    "\n Device Model: " + Build.MODEL + "\n Device Manufacturer: " + Build.MANUFACTURER + "\n\n";
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"rupanshuraman@hotmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
        intent.putExtra(Intent.EXTRA_TEXT, body);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.choose_email_client)));
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storedValue = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getBoolean(getString(R.string.key_check_fol_for_half_day), false) ? "true" : "false";
        storedValueHalfDayDelay = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getString(getString(R.string.key_delay_for_half_day_check), "1");
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    public static class MainPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);

            // name EditText change listener
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_display_name)));

            /*// notification preference change listener
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_notifications_new_message_ringtone)));*/

            Preference sharedPref = findPreference(getString(R.string.key_share));
            sharedPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    shareDialog();
                    return true;
                }

                private void shareDialog() {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, "Hi! This app helps me to get *faculty on leave reminders in morning automatically!!*.\nDownload it from here.\n" + getString(R.string.play_store_link));
                    startActivity(Intent.createChooser(intent, getString(R.string.download_share_link)));
                }
            });

            // feedback preference click listener
            Preference myPref = findPreference(getString(R.string.key_send_feedback));
            myPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    sendFeedback(getActivity());
                    return true;
                }
            });

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_is_24_hour_format)));

            Preference prefHalfDayReminder = findPreference(getString(R.string.key_check_fol_for_half_day));

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_window_size)));

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_select_max_retries)));

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_time_to_wait_for_active_connection)));

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_time_to_wait_for_networks)));

            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_delay_for_half_day_check)));

            Preference prefAdvancedSettingsButton = findPreference(getString(R.string.key_pref_advanced_settings));
            if (PreferenceManager.getDefaultSharedPreferences(prefHalfDayReminder.getContext())
                    .getBoolean(getString(R.string.key_scheduler_status), false)) {
                prefHalfDayReminder.setEnabled(true);
                prefAdvancedSettingsButton.setEnabled(true);

                bindPreferenceSummaryToValue(findPreference(getString(R.string.key_check_fol_for_half_day)));
            } else {
                prefHalfDayReminder.setSummary(R.string.warn_half_day_for_settings);
            }
        }
    }
}
