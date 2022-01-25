package org.emstrack.ambulance.util;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.text.DateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;
import java.util.Locale;

public class FormatUtils {

    public static float MILES_TO_KM = 1.609f;
    public static String METRIC = "metric";
    public static String IMPERIAL = "imperial";

    public static boolean isMetric(String value) {
        return METRIC.equals(value);
    }

    public static boolean isImperial(String value) {
        return IMPERIAL.equals(value);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static FormatStyle getFormatStyle(int format) {
        if (format == DateFormat.SHORT) {
            return FormatStyle.SHORT;
        } else if (format == DateFormat.MEDIUM) {
            return FormatStyle.MEDIUM;
        } else if (format == DateFormat.FULL) {
            return FormatStyle.FULL;
        }
        return FormatStyle.SHORT;
    }

    public static String formatDateTime(Calendar calendar, int format) {

        // set call updated on
        String updatedOn;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDateTime(getFormatStyle(format));
            updatedOn = dateFormatter.format(calendar
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime());
        } else {
            DateFormat dateFormat = DateFormat.getDateTimeInstance(format, format);
            updatedOn = dateFormat.format(calendar.getTime());
        }

        return updatedOn;
    }

    public static String formatDate(Calendar calendar, int format) {

        // set call updated on
        String updatedOn;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(getFormatStyle(format));
            updatedOn = dateFormatter.format(calendar
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate());
        } else {
            DateFormat dateFormat = DateFormat.getDateInstance(format);
            updatedOn = dateFormat.format(calendar.getTime());
        }

        return updatedOn;
    }

    public static String formatTime(Calendar calendar, int format) {

        // set call updated on
        String updatedOn;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedTime(getFormatStyle(format));
            updatedOn = dateFormatter.format(calendar
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime());
        } else {
            DateFormat dateFormat = DateFormat.getTimeInstance(format);
            updatedOn = dateFormat.format(calendar.getTime());
        }

        return updatedOn;
    }

    public static String formatDistance(double distance, String unitSystem) {
        if (isImperial(unitSystem)) {
            return String.format(Locale.getDefault(), "%.1f mi", distance / MILES_TO_KM );
        } else { //if (unitSystem.isMetric()) {
            return String.format(Locale.getDefault(), "%.1f km", distance);
        }
    }

    public static String formatDistance(float distance, String unitSystem) {
        if (isImperial(unitSystem)) {
            return String.format(Locale.getDefault(), "%.1f mi", distance / MILES_TO_KM );
        } else { //if (unitSystem.isMetric()) {
            return String.format(Locale.getDefault(), "%.1f km", distance);
        }
    }

}
