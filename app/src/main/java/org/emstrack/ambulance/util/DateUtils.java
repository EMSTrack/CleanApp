package org.emstrack.ambulance.util;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.text.DateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;

public class DateUtils {

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

}
