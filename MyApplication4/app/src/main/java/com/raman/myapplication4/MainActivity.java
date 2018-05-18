package com.raman.myapplication4;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
    ImageView dp;
    LinearLayout linearLayout, llSideBar;
    StringBuilder sb;
    ProgressBar bar;
    List<String> courseCode;
    List<String> teacherName;
    List<String> daySpan;
    Util util;
    TextView tvOnTopOfRecyclerView;
    Button buttonSelectDays;
    TextView tvRelativeTimeSpan;
    FloatingActionButton fab;
    Switch switchSchedulerStatus;
    TextView textViewSchedulerTimeDisplay;
    TextView textView_user_name;
    TextView tvWelcome;
    RecyclerView recyclerView;
    SharedPreferences sharedPref;
    SwipeRefreshLayout mySwipeRefreshLayout;
    private UserLoginTask mAuthTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("LPU UnderCover");
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        tvOnTopOfRecyclerView = (TextView) findViewById(R.id.tv_top_recycler);
        if (isFirstRun()) {
            Intent login = new Intent(this, LoginActivity.class);
            startActivity(login);
            finish();
            return;
        }
        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        recyclerView.setHasFixedSize(true);
        //layoutManager = new StaggeredGridLayoutManager(1,StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        util = new Util();
        recyclerView.setAdapter(new CustomAdapter(getApplicationContext(), sharedPref, util));

        if (recyclerView.getAdapter().getItemCount() == 0) {
            tvOnTopOfRecyclerView.setVisibility(View.VISIBLE);
            if (sharedPref.getLong(getString(R.string.key_time_stamp), -1) > 0)
                tvOnTopOfRecyclerView.setText(getString(R.string.no_faculty_on_leave) + "\n" + getString(R.string.swipe_to_refresh));
        }
        tvRelativeTimeSpan = (TextView) findViewById(R.id.relativeTimeSpan);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
        Log.i("MA", "text is" + tvOnTopOfRecyclerView.getText());
        if (!tvOnTopOfRecyclerView.getText().equals(getString(R.string.swipe_to_refresh))
                || recyclerView.getAdapter().getItemCount() > 0) {
            fab.setImageResource(R.drawable.ic_menu_share_white);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    shareTextIntent(sharedPref.getString(getString(R.string.key_share_text), getString(R.string.saved_result_not_found)));
                }
            });
            fab.setVisibility(View.VISIBLE);
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        navigationView.setNavigationItemSelectedListener(this);
        TextView textView_user = headerView.findViewById(R.id.textView_user);
        textView_user.setText(sharedPref.getString(getString(R.string.key_registration_number), "Registration Number not found"));
        textView_user_name = headerView.findViewById(R.id.textView_user_name);
        textView_user_name.setText(sharedPref.getString(getString(R.string.key_display_name), "UnderCover"));
        dp = headerView.findViewById(R.id.imageView);
        if (getIntent().getExtras() != null)
            sb = new StringBuilder(getIntent().getExtras().getString("absUrl", "default"));
        else sb = new StringBuilder("default");
        if (!"default".contentEquals(sb) && !sharedPref.getBoolean(getString(R.string.key_is_dp_saved), false)) {
            Thread download_dp = new Thread() {
                @Override
                public void run() {
                    InputStream input = null;
                    try {
                        input = new java.net.URL(sb.toString()).openStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // Decode Bitmap
                    final Bitmap bitmap = BitmapFactory.decodeStream(input);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dp.setImageBitmap(bitmap);
                        }
                    });
                    Log.i("LA", "Saving dp");
                    saveImageToInternalStorage(bitmap, "dp.png");
                    Log.i("LA", "dp saved");
                }
            };
            download_dp.start();
        } else
            dp.setImageBitmap(getDP());
        linearLayout = (LinearLayout) findViewById(R.id.ll_display_wallpaper);
        llSideBar = headerView.findViewById(R.id.ll_side_bar);
        if (new File(getFilesDir(), "dw.png").exists()) {
            Log.i("MA", "Another wallpaper exists setting it!");
            Log.i("MA", "Path is " + getFilesDir().getPath() + "/dw.png");

            Drawable drawable = Drawable.createFromPath(getFilesDir().getPath() + "/dw.png");
            /*if (sharedPref.getInt(getString(R.string.key_color_primary),-15108398)==-15108398)
            {
                Log.i("MA", "YO");
                saveProminentColors(((BitmapDrawable)drawable).getBitmap());
            }*/
            applyNewColorTheme();
            if (Build.VERSION.SDK_INT > 15)
                linearLayout.setBackground(drawable);
            else linearLayout.setBackgroundDrawable(drawable);
        }
        if (new File(getFilesDir(), "dsw.png").exists()) {
            Log.i("MA", "Another sidebar wallpaper exists setting it!");
            Log.i("MA", "Path is " + getFilesDir().getPath() + "/dsw.png");

            Drawable drawable = Drawable.createFromPath(getFilesDir().getPath() + "/dsw.png");
            if (Build.VERSION.SDK_INT > 15)
                llSideBar.setBackground(drawable);
            else llSideBar.setBackgroundDrawable(drawable);
        }
        mySwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        mySwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        checkFol(findViewById(R.id.swiperefresh));
                    }
                }
        );
        tvWelcome = (TextView) findViewById(R.id.tv_welcome);
        switchSchedulerStatus = (Switch) findViewById(R.id.tv_scheduler_status);
        textViewSchedulerTimeDisplay = (TextView) findViewById(R.id.tv_scheduler_time);
        buttonSelectDays = (Button) findViewById(R.id.button_select_days);

        //start service on app startup if scheduler active
        if (sharedPref.getBoolean(getString(R.string.key_scheduler_status), false)) {
            updateSchedulerCard(true);
            setAlarm(sharedPref, true);
            if (sharedPref.getBoolean(getString(R.string.key_check_fol_for_half_day), false))
                setAlarmForHalfDay();
        } else turnOffScheduler();

        if (sharedPref.getBoolean(getString(R.string.key_is_app_rated), true)) {
            if (sharedPref.getInt(getString(R.string.key_rate_app_counter), -1) % 40 == 0)
                showRateAppDialog();
            else {
                int rateApp = sharedPref.getInt(getString(R.string.key_rate_app_counter), 0);
                sharedPref.edit().putInt(getString(R.string.key_rate_app_counter), ++rateApp).apply();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        textView_user_name.setText(sharedPref.getString(getString(R.string.key_display_name), "UnderCover"));
        tvWelcome.setText("Welcome, " + sharedPref.getString(getString(R.string.key_display_name), "User") + "!");
        if (textViewSchedulerTimeDisplay.getVisibility() == View.VISIBLE)
            textViewSchedulerTimeDisplay.setText(String.format("%s (Change)",
                    util.convertToChoosenHourFormat(sharedPref.getInt(getString(R.string.key_selected_hour), 8)
                    + ":" + sharedPref.getInt(getString(R.string.key_selected_min), 0), sharedPref)));
        if (sharedPref.getLong(getString(R.string.key_time_stamp), 0) != 0)
            tvRelativeTimeSpan.setText(String.format("Updated %s",
                    DateUtils.getRelativeTimeSpanString(
                            sharedPref.getLong(getString(R.string.key_time_stamp), 0) > sharedPref.getLong(getString(R.string.key_time_stamp_scheduler), -1) ?
                                    sharedPref.getLong(getString(R.string.key_time_stamp), 0) : sharedPref.getLong(getString(R.string.key_time_stamp_scheduler), -1),
                            System.currentTimeMillis(), 0)));
        if (sharedPref.getBoolean(getString(R.string.key_scheduler_status), false)) {
            Log.i("MA", getString(R.string.notif_are_blocked_warn) + !NotificationManagerCompat.from(this).areNotificationsEnabled());
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                Snackbar.make(findViewById(android.R.id.content), R.string.notif_are_blocked_warn, Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.fix), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            }
                        }).show();
            }
        }
        if (Build.VERSION.SDK_INT >= 22
                && ((PowerManager) getSystemService(Context.POWER_SERVICE)).isPowerSaveMode()) {
            // Animations are disabled in power save mode, so just show a toast instead.
            Snackbar.make(findViewById(android.R.id.content), R.string.battery_saver_on_warn, Snackbar.LENGTH_LONG)
                    .setActionTextColor(Color.YELLOW)
                    .setAction(getString(R.string.fix), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent batterySaverIntent = null;
                            if (Build.VERSION.SDK_INT >= 22) {
                                batterySaverIntent = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
                            }
                            startActivity(batterySaverIntent);
                        }
                    }).show();
        }
    }

    private void shareTextIntent(String shareText) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "*Faculty on Leave*\n--------------------------\n" + shareText
                + "\n*Checked by LPU UnderCover app*\n" + getString(R.string.play_store_link));
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
    }

    private void showRateAppDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.enjoying_app_rate).setIcon(R.drawable.ic_star_rate_black_18px)
                .setPositiveButton(R.string.rate_now, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        marketIntent();
                        sharedPref.edit().putBoolean(getString(R.string.key_is_app_rated), false).apply();
                        Toast.makeText(MainActivity.this, R.string.thanks_for_rating, Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton(R.string.dont_show_again, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                sharedPref.edit().putBoolean(getString(R.string.key_is_app_rated), false).apply();
            }
        }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                sharedPref.edit().putInt(getString(R.string.key_rate_app_counter), 1).apply();
            }
        }).show();
    }

    @Override
    public void onBackPressed() {
        if (mAuthTask != null)
            mAuthTask.cancel(true);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public boolean isFirstRun() {
        return sharedPref.getString(getString(R.string.key_registration_number), "a").equals("a");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.refresh_fol) {
            checkFol(findViewById(R.id.swiperefresh));
            return true;
        } else if (id == R.id.change_dp) {
            chooseDisplayPicture(item.getActionView());
        } else if (id == R.id.change_dw) {
            chooseDisplayWallpaper(item.getActionView());
        } else if (id == R.id.change_dsw) {
            chooseDisplaySideBarWallpaper(item.getActionView());
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_change_pass) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        } else if (id == R.id.nav_about) {
            showAboutAppDialog();
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_share) {
            shareIntent();
        } else if (id == R.id.nav_feedback) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("plain/text");
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.my_hotmail_email)});
            i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback));
            startActivity(Intent.createChooser(i, "Send mail..."));
        } else if (id == R.id.nav_rate) {
            marketIntent();
            Toast.makeText(this, R.string.thanks_for_rating, Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_logout) {
            new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.myDialog)).setTitle(R.string.string_log_out)
                    .setIcon(R.drawable.ic_log_out_new_black_24px)
                    .setMessage(R.string.sure_to_logout_warning)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            doLogout();
                        }
                    }).setNegativeButton(android.R.string.no, null).show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showAboutAppDialog() {
        LayoutInflater li = LayoutInflater.from(MainActivity.this);
        View promptsView = li.inflate(R.layout.about_page_layout, null);
        TextView changelog = promptsView.findViewById(R.id.changelog);
        changelog.setMovementMethod(LinkMovementMethod.getInstance());
        TextView rupanshu_raman_developed = promptsView.findViewById(R.id.rupanshu_raman_developed);
        rupanshu_raman_developed.setMovementMethod(LinkMovementMethod.getInstance());
        TextView sid_tested = promptsView.findViewById(R.id.sid_tested);
        sid_tested.setMovementMethod(LinkMovementMethod.getInstance());
        TextView faq = promptsView.findViewById(R.id.FAQ);
        faq.setMovementMethod(LinkMovementMethod.getInstance());
        TextView request_feature_bug = promptsView.findViewById(R.id.request_feature_bug);
        request_feature_bug.setMovementMethod(LinkMovementMethod.getInstance());
        TextView permission_details = promptsView.findViewById(R.id.permission_details);
        permission_details.setMovementMethod(LinkMovementMethod.getInstance());
        TextView privacy_policy_textView = promptsView.findViewById(R.id.privacy_policy_text);
        privacy_policy_textView.setMovementMethod(LinkMovementMethod.getInstance());
        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.myDialog))
                .setView(promptsView)
                .setIcon(R.drawable.ic_info_black_24dp)
                .setTitle(R.string.about_app)
                .setPositiveButton(android.R.string.ok, null)
                .create().show();
    }

    private void shareIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_with_friends_text)
                + getString(R.string.play_store_link));
        startActivity(Intent.createChooser(intent, getString(R.string.download_share_link)));
    }

    private void marketIntent() {
        Intent marketIntent = new Intent();
        marketIntent.setAction(Intent.ACTION_VIEW);
        marketIntent.setData(Uri.parse("market://details?id=com.raman.myapplication4"));
        startActivity(marketIntent);
    }

    private void doLogout() {
        File dir = getFilesDir();
        new File(dir, "dp.png").delete();
        sharedPref.edit().clear().apply();
        startActivity(new Intent(this, LoginActivity.class));
        stopService(new Intent(MainActivity.this, NotificationService.class));
        finish();
    }

    public Bitmap getDP() {
        Bitmap thumbnail = null;
        try {
            thumbnail = BitmapFactory.decodeStream(new FileInputStream(this.getFileStreamPath("dp.png")));
        } catch (Exception ex) {
            Log.i("dp on Internal Storage", ex.getMessage());
        }
        return thumbnail;
    }

    public void checkFol(View v) {
        if (mAuthTask != null)
            mAuthTask.cancel(true);
        tvOnTopOfRecyclerView.setVisibility(View.GONE);
        tvRelativeTimeSpan.setVisibility(View.GONE);
        tvOnTopOfRecyclerView.setText(getString(R.string.swipe_to_refresh));
        fab.setVisibility(View.VISIBLE);
        fab.setImageResource(R.drawable.ic_cancel);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAuthTask != null)
                    mAuthTask.cancel(true);
                bar.setVisibility(View.GONE);
                mySwipeRefreshLayout.setRefreshing(false);
                Snackbar.make(view, getString(R.string.task_cancelled), Snackbar.LENGTH_SHORT).show();
                fab.setVisibility(View.GONE);
            }
        });
        mAuthTask = new UserLoginTask(sharedPref.getString(getString(R.string.key_registration_number), "a")
                , sharedPref.getString(getString(R.string.key_password), "a"));
        sb = new StringBuilder();
        mAuthTask.execute((Void) null);
    }

    public void showTimePickerDialog(View view) {
        final Calendar mcurrentTime = Calendar.getInstance();
        int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int minute = mcurrentTime.get(Calendar.MINUTE);
        TimePickerDialog mTimePicker;
        mTimePicker = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                sharedPref.edit().putInt(getString(R.string.key_selected_hour), selectedHour)
                        .putInt(getString(R.string.key_selected_min), selectedMinute)
                        .putString(getString(R.string.key_today_done_date), "0")
                        .putBoolean(getString(R.string.key_scheduler_status), true)
                        .apply();
                setAlarm(sharedPref, false);
                updateSchedulerCard(true);
                if (!sharedPref.getBoolean(getString(R.string.key_check_fol_for_half_day), false)) {
                    Snackbar.make(findViewById(android.R.id.content), R.string.want_half_day_reminders, Snackbar.LENGTH_LONG)
                            .setAction(android.R.string.ok, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    enableHalfDayReminder();
                                }

                                private void enableHalfDayReminder() {
                                    sharedPref.edit().putBoolean(getString(R.string.key_check_fol_for_half_day), true).apply();
                                    Snackbar.make(findViewById(android.R.id.content), R.string.will_be_delivered_at_12_15, Snackbar.LENGTH_LONG)
                                            .setAction(R.string.change, new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                                                }
                                            }).show();
                                }
                            }).show();
                }
                Log.i("MA", "time selected" + mcurrentTime.getTime());
            }
        }, hour, minute, sharedPref.getBoolean(getString(R.string.key_is_24_hour_format),
                sharedPref.getBoolean(getString(R.string.key_is_24_hour_format), false)));
        mTimePicker.setIcon(R.drawable.ic_schedule_black_24px);
        mTimePicker.setTitle(R.string.select_time_for_reminder);
        mTimePicker.show();
    }

    private void updateSchedulerCard(boolean enabledStatus) {
        switchSchedulerStatus.setChecked(enabledStatus);
        buttonSelectDays.setVisibility(enabledStatus ? View.VISIBLE : View.GONE);
        textViewSchedulerTimeDisplay.setVisibility(enabledStatus ? View.VISIBLE : View.GONE);
        if (enabledStatus)
            textViewSchedulerTimeDisplay.setText(String.format("%s (Change)"
                    , util.convertToChoosenHourFormat(sharedPref.getInt(getString(R.string.key_selected_hour), 8)
                    + ":" + sharedPref.getInt(getString(R.string.key_selected_min), 0), sharedPref)));
    }

    private void turnOffScheduler() {
        ComponentName receiver = new ComponentName(this, AppStarter.class);
        PackageManager pm = getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        final Intent myIntent = new Intent(MainActivity.this, Notify.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, myIntent, 0);
        alarmManager.cancel(pendingIntent);
        PendingIntent pendingIntentHalfDay = PendingIntent.getBroadcast(getApplicationContext(), 2, myIntent, 0);
        alarmManager.cancel(pendingIntentHalfDay);
        Log.i("SA", "Half day Reminder alarm cancelled");
        updateSchedulerCard(false);
        stopService(new Intent(MainActivity.this, NotificationService.class));
        sharedPref.edit().putBoolean(getString(R.string.key_scheduler_status), false).apply();
    }

    public void setAlarm(SharedPreferences sharedPref, boolean byOnCreateMethod) {
        ComponentName receiver = new ComponentName(this, AppStarter.class);
        PackageManager pm = getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        final Intent myIntent = new Intent(MainActivity.this, Notify.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, myIntent, 0);
        alarmManager.cancel(pendingIntent);
        Calendar mcurrentTime = Calendar.getInstance();
        //TODO add jitter
        mcurrentTime.set(Calendar.HOUR_OF_DAY, sharedPref.getInt(getString(R.string.key_selected_hour), 8));
        mcurrentTime.set(Calendar.MINUTE, sharedPref.getInt(getString(R.string.key_selected_min), 0));

        long[] timeFrameSize = new long[]{300000, 600000, AlarmManager.INTERVAL_FIFTEEN_MINUTES, 1200000, AlarmManager.INTERVAL_HALF_HOUR};
        long interval = timeFrameSize[Integer.parseInt(sharedPref.getString(getString(R.string.key_window_size), "2"))];
        long offset = byOnCreateMethod && util.isTodayDone(getApplicationContext(), sharedPref) ? interval : 0;
        Log.i("MA", "Time frame got" + interval);
        if (Build.VERSION.SDK_INT >= 19) {
            if (Calendar.getInstance().getTimeInMillis() - mcurrentTime.getTimeInMillis() + offset < 0
                    && sharedPref.getString(getString(R.string.key_selected_days), Arrays.toString(new int[]{0, 1, 2, 3, 4}))
                    .contains(String.valueOf(util.getDayAccToSaved(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))))) {
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, mcurrentTime.getTimeInMillis() - interval, interval, pendingIntent);
                Snackbar.make(findViewById(android.R.id.content),
                        "Today reminder " + util.getRelativeTimeString(getApplicationContext(), sharedPref),
                        Snackbar.LENGTH_SHORT).show();
                Log.i("MA", "Today set Reminder");
            } else {
                //past time set for next day
                Log.i("MA", "one day later set Reminder");
                Snackbar.make(findViewById(android.R.id.content),
                        "Reminder on NEXT selected day " + util.getRelativeTimeString(getApplicationContext(), sharedPref),
                        Snackbar.LENGTH_SHORT).show();
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, mcurrentTime.getTimeInMillis() - interval + AlarmManager.INTERVAL_DAY, interval, pendingIntent);
            }
        } else {
            //For older versions setRepeating alarms are exact
            Log.i("MA", "set repeating for lower than 4.4 android");
            if (Calendar.getInstance().getTimeInMillis() < mcurrentTime.getTimeInMillis()
                    && sharedPref.getString(getString(R.string.key_selected_days), Arrays.toString(new int[]{0, 1, 2, 3, 4}))
                    .contains(String.valueOf(util.getDayAccToSaved(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))))) {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, mcurrentTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
                Snackbar.make(findViewById(android.R.id.content),
                        "Today reminder " + util.getRelativeTimeString(getApplicationContext(), sharedPref),
                        Snackbar.LENGTH_SHORT).show();
                Log.i("MA", "Today set Reminder for 4.4");
            } else {
                Log.i("MA", "one day later set Reminder for 4.4");
                Snackbar.make(findViewById(android.R.id.content),
                        "Reminder on NEXT selected day " + util.getRelativeTimeString(getApplicationContext(), sharedPref),
                        Snackbar.LENGTH_SHORT).show();
            }
        }
        startService(new Intent(MainActivity.this, NotificationService.class));
    }

    private void setAlarmForHalfDay() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        final Intent myIntent = new Intent(MainActivity.this, Notify.class);
        myIntent.putExtra(getString(R.string.requestCode), "2");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 2, myIntent, 0);
        alarmManager.cancel(pendingIntent);
        Calendar mcurrentTime = Calendar.getInstance();
        //TODO add jitter
        mcurrentTime.set(Calendar.HOUR_OF_DAY, 12);
        mcurrentTime.set(Calendar.MINUTE, 0);

        long[] timeFrameSize = new long[]{300000, 600000, AlarmManager.INTERVAL_FIFTEEN_MINUTES, 1200000, AlarmManager.INTERVAL_HALF_HOUR};
        long interval = timeFrameSize[Integer.parseInt(sharedPref.getString(getString(R.string.key_window_size), "2"))];
        int[] addDelayArray = new int[]{0, 15, 30, 45, 60, 90, 120, 180};
        int addDelay = addDelayArray[Integer.parseInt(sharedPref.getString(getString(R.string.key_delay_for_half_day_check), "1"))];
        mcurrentTime.add(Calendar.MINUTE, addDelay);
        Log.i("MA", "half day time is " + mcurrentTime.getTime());
        if (Build.VERSION.SDK_INT >= 19) {
            if (Calendar.getInstance().getTimeInMillis() - mcurrentTime.getTimeInMillis() < 0
                    && !util.isHalfDayTodayDone(getApplicationContext(), sharedPref)
                    && sharedPref.getString(getString(R.string.key_selected_days), Arrays.toString(new int[]{0, 1, 2, 3, 4}))
                    .contains(String.valueOf(util.getDayAccToSaved(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))))) {
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, mcurrentTime.getTimeInMillis() - interval, interval, pendingIntent);
                Log.i("MA", "Half day Today set Reminder");
            } else {
                //past time set for next day
                Log.i("MA", "half day one day later set Reminder");
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, mcurrentTime.getTimeInMillis() - interval + AlarmManager.INTERVAL_DAY, interval, pendingIntent);
            }
        } else {
            //For older versions setRepeating alarms are exact
            Log.i("MA", "half day set repeating for lower than 4.4 android");
            if (Calendar.getInstance().getTimeInMillis() < mcurrentTime.getTimeInMillis()) {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, mcurrentTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
                Log.i("MA", "Half day Today set Reminder for 4.4");
            } else {
                Log.i("MA", "one day later set Reminder for 4.4");
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (switchSchedulerStatus.isChecked()) {
            new AlertDialog.Builder(this).setTitle(R.string.turn_off_scheduler_warn)
                    .setIcon(R.drawable.ic_schedule_black_24px)
                    .setMessage(R.string.not_receive_fol_warn)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            turnOffScheduler();
                        }
                    }).setNegativeButton(android.R.string.no, null).show();
        } else {
            showTimePickerDialog(view);
        }
    }

    public void openSchedulerSettings(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        boolean[] checkedTopics = util.convertStringToBooleanArray(sharedPref.getString(getString(R.string.key_selected_days)
                , Arrays.toString(new int[]{0, 1, 2, 3, 4})));
        final ArrayList<Integer> finalMSelectedItems = getValuesFromBooleanArray(checkedTopics);
        String[] daysOfWeekArray = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        builder.setTitle(R.string.select_days_long)
                .setIcon(R.drawable.ic_schedule_black_24px)
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected
                .setMultiChoiceItems(daysOfWeekArray, checkedTopics,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
                                    finalMSelectedItems.add(which);
                                } else if (finalMSelectedItems.contains(which)) {
                                    // Else, if the item is already in the array, remove it
                                    finalMSelectedItems.remove(Integer.valueOf(which));
                                }
                                Log.d("MA", String.valueOf(finalMSelectedItems));
                            }
                        })
                // Set the action buttons
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK, so save the mSelectedItems results somewhere
                        // or return them to the component that opened the dialog
                        Log.d("MA", String.valueOf(finalMSelectedItems));
                        saveSelection(finalMSelectedItems);
                        setAlarm(sharedPref, false);
                        Log.d("MA", String.valueOf(finalMSelectedItems));
                    }
                }).setNegativeButton(getString(android.R.string.cancel), null).create().show();
    }

    private void saveSelection(ArrayList<Integer> mSelectedItems) {
        if (mSelectedItems.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), R.string.scheduler_turned_off_nothing_selected
                    , Snackbar.LENGTH_LONG).show();
            mSelectedItems.addAll(Arrays.asList(0, 1, 2, 3, 4));
            turnOffScheduler();
        }
        sharedPref.edit()
                .putString(getString(R.string.key_selected_days), mSelectedItems.toString())
                .apply();
    }

    private ArrayList<Integer> getValuesFromBooleanArray(boolean[] checkedTopics) {
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0; i < 7; i++)
            if (checkedTopics[i])
                result.add(i);
        return result;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getPhotoIntent(1);
            }
        } else if (requestCode == 2) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getPhotoIntent(2);
            }
        }
    }

    public void getPhotoIntent(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, requestCode);
    }

    public void chooseDisplayPicture(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            } else {
                getPhotoIntent(1);
            }
        } else getPhotoIntent(1);
    }

    private void chooseDisplaySideBarWallpaper(View actionView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
            } else {
                getPhotoIntent(3);
            }
        } else {
            getPhotoIntent(3);
        }
    }

    public void chooseDisplayWallpaper(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
            } else {
                getPhotoIntent(2);
            }
        } else {
            getPhotoIntent(2);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                Log.i("MA", "width: " + bitmap.getWidth() + " height: " + bitmap.getHeight());
                //TODO add crop image option
                if (bitmap.getHeight() * bitmap.getWidth() > 435600) {
                    Toast.makeText(this, "Image too big for display Picture!", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveImageToInternalStorage(bitmap, "dp.png");

                dp.setImageBitmap(bitmap);
            } catch (IOException e) {
                Toast.makeText(this, "I/O exception", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

        } else if (requestCode == 2 && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                Log.i("MA", "width: " + bitmap.getWidth() + " height: " + bitmap.getHeight());
                if (bitmap.getHeight() * bitmap.getWidth() > 1865956) {
                    Toast.makeText(this, "Image too big for wallpaper!", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveProminentColors(bitmap);
                saveImageToInternalStorage(bitmap, "dw.png");

                Drawable drawable = new BitmapDrawable(getResources(), bitmap);

                if (Build.VERSION.SDK_INT > 15)
                    linearLayout.setBackground(drawable);
                else linearLayout.setBackgroundDrawable(drawable);
            } catch (IOException e) {
                Toast.makeText(this, "I/O exception", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else if (requestCode == 3 && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                Log.i("MA", "width: " + bitmap.getWidth() + " height: " + bitmap.getHeight());
                if (bitmap.getHeight() * bitmap.getWidth() > 1865956) {
                    Toast.makeText(this, "Image too big for sidebar wallpaper!", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveImageToInternalStorage(bitmap, "dsw.png");

                Drawable drawable = new BitmapDrawable(getResources(), bitmap);

                if (Build.VERSION.SDK_INT > 15)
                    llSideBar.setBackground(drawable);
                else llSideBar.setBackgroundDrawable(drawable);
            } catch (IOException e) {
                Toast.makeText(this, "I/O exception", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    private void saveProminentColors(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT > 22) {
            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    int color = palette.getVibrantColor(ContextCompat
                            .getColor(MainActivity.this, R.color.colorPrimary));
                    int darkColor = palette.getDarkVibrantColor(ContextCompat
                            .getColor(MainActivity.this, R.color.colorPrimaryDark));
                    Log.i("MA", "saving >22 Color: " + color + " darkColor: " + darkColor);
                    sharedPref.edit().putInt(getString(R.string.key_color_primary), color)
                            .putInt(getString(R.string.key_color_primary_dark), darkColor).apply();
                    applyNewColorTheme();
                }
            });
        } else {
            Palette.generateAsync(bitmap, new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    int color = palette.getVibrantColor(getResources().getColor(R.color.colorPrimary));
                    Log.i("MA", "saving <22 Color: " + color);
                    sharedPref.edit().putInt(getString(R.string.key_color_primary), color).apply();
                    applyNewColorTheme();
                }
            });
        }
    }

    private void applyNewColorTheme() {
        Log.i("MA", "Primary color int is" + getResources().getColor(R.color.colorPrimary));
        Log.i("MA", "applying new theme");
        //TODO replace by above int value
        int color = sharedPref.getInt(getString(R.string.key_color_primary), getResources().getColor(R.color.colorPrimary));
        int darkColor = sharedPref.getInt(getString(R.string.key_color_primary_dark), getResources().getColor(R.color.colorPrimaryDark));
        Log.i("MA", "color: " + color);
        Log.i("MA", "dark Color: " + darkColor);
        if (Build.VERSION.SDK_INT > 22) {
            Log.i("MA", "applying theme >22");
            if (Build.VERSION.SDK_INT >= 21) {
                getWindow().setStatusBarColor(darkColor);
            }
            if (getSupportActionBar() != null)
                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color));
            fab.setBackgroundTintList(ColorStateList.valueOf(color));
        } else {
            Log.i("MA", "applying theme <22");
            if (getSupportActionBar() != null)
                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color));
            fab.setBackgroundTintList(ColorStateList.valueOf(color));
        }
    }

    boolean saveImageToInternalStorage(Bitmap image, String file_name) {
        try {
            // Use the compress method on the Bitmap object to write image to
            // the OutputStream
            FileOutputStream fos = getApplicationContext().openFileOutput(file_name, Context.MODE_PRIVATE);
            // Writing the bitmap to the output stream
            image.compress(Bitmap.CompressFormat.WEBP, 50, fos);
            fos.close();
            sharedPref.edit().putBoolean(getString(R.string.key_is_dp_saved), true).apply();
            return true;
        } catch (Exception e) {
            Log.e("saveToInternalStorage()", e.getMessage());
            return false;
        }
    }

    private class UserLoginTask extends AsyncTask<Void, Integer, Boolean> {
        private final String mRegistrationNumber;
        private final String mPassword;
        int res;
        long start = System.currentTimeMillis();
        StringBuilder shareText = new StringBuilder();

        UserLoginTask(String registrationNumber, String password) {
            mRegistrationNumber = registrationNumber;
            mPassword = password;
            bar = (ProgressBar) findViewById(R.id.progress_fol_check);
            bar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (bar != null) {
                bar.setProgress(values[0]);
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                publishProgress(10);
                Connection.Response response = Jsoup.connect(getString(R.string.ums_login_url))
                        .method(Connection.Method.GET)
                        .execute();
                publishProgress(20);
                final String viewstate = response.parse().select("input[name=__VIEWSTATE]").first().attr("value");
                if (response.statusCode() == 302 || response.parse().text().contains(getString(R.string.oops_ums_server_down_error)) || viewstate == null) {
                    res = 2;
                    return false;
                }
                publishProgress(30);
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
                    res = 7;
                    return false;
                }
                publishProgress(50);
                if (response.parse().title().equals(getString(R.string.ums_login_page_title))) {
                    res = 1;
                    return false;
                }
                Document document = response.parse();
                if (document.select(getString(R.string.ums_fol_table_id)).text().equals(getString(R.string.no_faculty_on_leave))) {
                    shareText.append(getString(R.string.no_faculty_on_leave));
                    res = 5;
                    publishProgress(100);
                    return true;
                }
                Element table = document.select(getString(R.string.ums_fol_table_id)).last();
                Elements tds;
                publishProgress(70);
                courseCode = new ArrayList<>();
                teacherName = new ArrayList<>();
                daySpan = new ArrayList<>();
                for (Element row : table.select("tr")) {
                    tds = row.select("td");
                    if (tds.size() > 0) {
                        courseCode.add(tds.get(0).text());
                        teacherName.add(tds.get(2).text());
                        daySpan.add(tds.get(3).text());
                        //TODO performance
                        shareText.append("\u25CF").append(tds.get(2).text()).append("\t").append(tds.get(3).text()).append("\n");
                    }
                }
                publishProgress(80);
                calculateElapsedTime(start);
                publishProgress(100);
            } catch (SocketTimeoutException timeout) {
                res = 6;
                timeout.printStackTrace();
                return false;
            } catch (UnknownHostException uhe) {
                res = 3;
                uhe.printStackTrace();
                return false;
            } catch (IOException e) {
                res = 4;
                e.printStackTrace();
                return false;
            } catch (NullPointerException npe) {
                res = 7;
                npe.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mySwipeRefreshLayout.setRefreshing(false);
            if (success) {
                Log.i("MA", "result" + shareText);
                if (mAuthTask != null)
                    Snackbar.make(findViewById(android.R.id.content), String.format(Locale.ENGLISH
                            , "Checked in %ds", (int) mAuthTask.calculateElapsedTime(start)), Snackbar.LENGTH_SHORT).show();
                if (courseCode != null) {
                    tvOnTopOfRecyclerView.setVisibility(View.GONE);
                    recyclerView.setAdapter(new CustomAdapter(getApplicationContext(), courseCode, teacherName, daySpan));
                } else {
                    tvOnTopOfRecyclerView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    tvOnTopOfRecyclerView.setText(getString(R.string.no_faculty_on_leave) + "\n" + getString(R.string.swipe_to_refresh));
                }
                tvRelativeTimeSpan.setVisibility(View.VISIBLE);
                tvRelativeTimeSpan.setText(String.format("Updated %s", DateUtils.getRelativeTimeSpanString(start
                        , System.currentTimeMillis(), 0)));
                util.saveFoLResult(sharedPref, courseCode, teacherName, daySpan, shareText.toString());
                fab.setVisibility(View.VISIBLE);
                fab.setImageResource(R.drawable.ic_menu_share_white);
                fab.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.anim_zoom_big_small_share));
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        shareTextIntent(shareText.toString());
                    }
                });
                bar.setVisibility(View.GONE);
                closeSession(MainActivity.this);
            } else {
                /*
                TODO transfer switch block to a runOnUiThread and execute with res pass value Error Codes
                0=Don't assign zero as its default value of res(integer in java)
				1=Wrong Username or password
                2=UMS server busy
                3=No Internet Access
                4=Basic Input/Output Error
                5=Faculty on leave
                6=Connection timed out
                7=UMS structure changed
                 */
                tvOnTopOfRecyclerView.setVisibility(View.VISIBLE);
                fab.setVisibility(View.GONE);
                bar.setVisibility(View.GONE);
                switch (res) {
                    case 1:
                        Snackbar.make(findViewById(android.R.id.content), R.string.try_again, Snackbar.LENGTH_LONG)
                                .setAction("Change", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                    }
                                })
                                .show();
                        break;
                    case 2:
                        Snackbar.make(findViewById(android.R.id.content)
                                , getString(R.string.ums_server_busy), Snackbar.LENGTH_LONG).show();
                        break;
                    case 3:
                        Snackbar.make(findViewById(android.R.id.content), getString(R.string.no_internet_access), Snackbar.LENGTH_LONG).show();
                        break;
                    case 4:
                        Snackbar.make(findViewById(android.R.id.content), getString(R.string.network_error), Snackbar.LENGTH_SHORT).show();
                        break;
                    case 6:
                        Snackbar.make(findViewById(android.R.id.content), getString(R.string.connection_timed_out), Snackbar.LENGTH_LONG)
                                .setAction(R.string.try_again, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        checkFol(view);
                                    }
                                }).show();
                        break;
                    case 7:
                        //TODO add action market intent
                        Snackbar.make(findViewById(android.R.id.content), R.string.try_again, Snackbar.LENGTH_LONG).show();
                }
            }
        }

        private void closeSession(final MainActivity context) {
            Thread logout_thread = new Thread() {
                @Override
                public void run() {
                    try {
                        Jsoup.connect(getString(R.string.ums_login_url))
                                .method(Connection.Method.GET)
                                .execute();
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Snackbar.make(findViewById(android.R.id.content)
                                        , getString(R.string.string_session_closed), Snackbar.LENGTH_LONG).show();
                            }
                        });
                        Log.i("MA", "session closed");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            logout_thread.start();
        }

        private double calculateElapsedTime(long start) {
            return (double) (System.currentTimeMillis() - start) / 1000.0;
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
        }
    }
}