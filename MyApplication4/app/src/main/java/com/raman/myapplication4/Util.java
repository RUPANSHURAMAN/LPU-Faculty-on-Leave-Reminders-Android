package com.raman.myapplication4;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by RUPANSHU and ${PACKAGE_NAME} and ${PACKAGE_NAME} on 15-Jul-17.
 */

class Util {

    String convertToChoosenHourFormat(String s, SharedPreferences sharedPref) {
        try {
            final SimpleDateFormat sdf = new SimpleDateFormat("H:mm");
            final Date dateObj = sdf.parse(s);
            if (sharedPref.getBoolean("key_is_24_hour_format", false)) {
                return new SimpleDateFormat("HH:mm").format(dateObj);
            } else {
                return new SimpleDateFormat("hh:mm aa").format(dateObj);
            }
        } catch (final ParseException e) {
            e.printStackTrace();
        }
        return "Error,Try Again";
    }

    List<String> convertStringArrayToList(String categoryPositionCounter) {
        List<String> strings = Arrays.asList(categoryPositionCounter.replace("[", "").replace("]", "").split(", "));
        Log.i("Util", "" + strings);
        return strings;
    }

    boolean[] convertStringToBooleanArray(String selectedWeek) {
        boolean[] boolArray = new boolean[7];
        selectedWeek = selectedWeek.replace("[", "").replace("]", "").replaceAll(", ", "");
        Log.d("Util", "DEBUG!=" + selectedWeek);
        for (int i = 0; i < selectedWeek.length(); i++) {
            boolArray[Character.getNumericValue(selectedWeek.charAt(i))] = true;
        }
        return boolArray;
    }

    void saveFoLResult(SharedPreferences sharedPref, List<String> courseCode, List<String> teacherName, List<String> daySpan, String shareText) {
        if (courseCode != null) {
            sharedPref.edit().putString("key_course_codes", courseCode.toString())
                    .putString("key_teacher_names", teacherName.toString())
                    .putString("key_day_spans", daySpan.toString()).apply();
        } else {
            sharedPref.edit().remove("key_course_codes").remove("key_teacher_names").remove("key_day_spans").apply();
        }
        sharedPref.edit().putLong("key_time_stamp", System.currentTimeMillis()).putString("key_share_text", shareText).apply();
    }

    String getRelativeTimeString(Context context, SharedPreferences sharedPref) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, sharedPref.getInt("key_selected_hour", 8));
        calendar.set(Calendar.MINUTE, sharedPref.getInt("key_selected_min", 0));
        calendar.add(Calendar.DAY_OF_MONTH, getNextDayDifference(context, sharedPref, calendar));
        Log.i("NService", "hour=" + calendar.get(Calendar.HOUR_OF_DAY) + " minute=" + calendar.get(Calendar.MINUTE)
                + " dayOfMonth" + calendar.get(Calendar.DAY_OF_MONTH) + " month=" + calendar.get(Calendar.MONTH) + " year" + calendar.get(Calendar.YEAR));
        Log.i("Util", String.valueOf(DateUtils.getRelativeTimeSpanString(calendar.getTimeInMillis(), Calendar.getInstance().getTimeInMillis(), 0)));
        return String.valueOf(DateUtils.getRelativeTimeSpanString(calendar.getTimeInMillis(), Calendar.getInstance().getTimeInMillis(), 0));
    }

    private int getNextDayDifference(Context context, SharedPreferences sharedPref, Calendar calendar) {
        String[] daysOfWeek = sharedPref.getString("key_selected_days", Arrays.toString(new int[]{0, 1, 2, 3, 4})).replace("[", "").replace("]", "").split(", ");
        int[] daysOfWeekInt = new int[daysOfWeek.length];
        for (int i = 0; i < daysOfWeek.length; ++i) {
            daysOfWeekInt[i] = (Integer.parseInt(daysOfWeek[i]) + 2) % 7;
            if (daysOfWeekInt[i] == 0)
                daysOfWeekInt[i] = 7;
        }
        Arrays.sort(daysOfWeekInt);
        Log.i("Util", "int arrays is" + Arrays.toString(daysOfWeekInt));
        Calendar calendarNow = Calendar.getInstance();
        int todayDay = calendarNow.get(Calendar.DAY_OF_WEEK);
        if (calendar.getTimeInMillis() > calendarNow.getTimeInMillis())
            Log.i("Util", "Scheduler>Now");
        Log.i("Util", "Scheduler time" + calendar.getTimeInMillis() + " NOw" + calendarNow.getTimeInMillis());
        if (!DateUtils.isToday(sharedPref.getLong("key_time_stamp_scheduler", System.currentTimeMillis()) - getSchedulerTimeOffset(sharedPref)))
            if (calendar.getTimeInMillis() > calendarNow.getTimeInMillis() && !isTodayDone(context, sharedPref))
            return 0;
        int diff;
        for (int i : daysOfWeekInt)
            if (i > todayDay) {
                diff = i - todayDay;
                Log.i("NService", "The diff is " + diff);
                return diff;
            }
        diff = 7 - (todayDay - daysOfWeekInt[0]);
        Log.i("NService", "The DIFF lAST is " + diff);//5 1 7-(5-1) i.e. 7-(input-output)
        return diff;
    }

    boolean isTodayDone(Context context, SharedPreferences sharedPref) {
        return sharedPref.getString(context.getString(R.string.key_today_done_date), "-1").equals(DateFormat.format("dd", new Date()));
    }

    boolean isHalfDayTodayDone(Context context, SharedPreferences sharedPref) {
        return sharedPref.getString(context.getString(R.string.key_today_done_date_half_day), "-1").equals(DateFormat.format("dd", new Date()));
    }

    private long getSchedulerTimeOffset(SharedPreferences sharedPref) {
        return (sharedPref.getInt("key_selected_hour", 8) * 3600000) + (sharedPref.getInt("key_selected_min", 0) * 60000);
    }

    int getDayAccToSaved(int dayOfWeek) {
        return ((dayOfWeek - 2) + 7) % 7;
    }
}
