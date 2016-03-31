package de.qabel.qabelbox.helper;

import android.content.Context;
import android.content.res.Resources;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.regex.Pattern;

import de.qabel.qabelbox.R;

/**
 * Created by danny on 14.01.2016.
 */
public class Formatter {

    private static final long KB = 1024;
    private static final long MB = 1024 * 1024;
    private static final long GB = 1024 * 1024 * 1024;

    private static final DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    private static final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
    private static final DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

    public static String formatFileSizeHumanReadable(Context context, long filesize) {

        String result;
        DecimalFormat df = new DecimalFormat("#.##");
        Resources res = context.getResources();
        if (filesize < KB) {
            result = filesize + " " + res.getString(R.string.unit_filesyste_bytes);
        } else if (filesize < MB)
            result = df.format((double) filesize / KB) + " " + res.getString(R.string.unit_filesyste_kb);
        else if (filesize < GB)
            result = df.format((double) filesize / MB) + " " + res.getString(R.string.unit_filesyste_mb);
        else {
            result = df.format((double) filesize / GB) + " " + res.getString(R.string.unit_filesyste_gb);
        }
        return result.replace(",", ".");
    }

    public static String formatDateShort(Date date) {

        return dateFormat.format(date);
    }

    public static String formatDateShort(long date) {

        return formatDateShort(new Date(date));
    }

    public static String formatDateTimeShort(Date date) {

        return dateTimeFormat.format(date);
    }

    public static String formatDateTimeShort(long date) {

        return formatDateTimeShort(new Date(date));
    }

    /**
     * Formats a Date to a String like "Fr. 20:35" or "15.02.15 20:35" if the date is more than a week in the past.
     *
     * @param time
     * @return
     */
    public static String formatDateTimeString(long time) {
        Calendar current = GregorianCalendar.getInstance();
        Calendar date = GregorianCalendar.getInstance();
        date.setTimeInMillis(time);
        Date dateObj = new Date(time);

        StringBuilder dateString = new StringBuilder();
        if (current.get(GregorianCalendar.DAY_OF_YEAR) - date.get(GregorianCalendar.DAY_OF_YEAR) < 7) {
            dateString.append(date.getDisplayName(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.SHORT, Locale.getDefault()));
        } else {
            dateString.append(dateFormat.format(dateObj));
        }
        dateString.append(" ");
        dateString.append(timeFormat.format(dateObj));

        return dateString.toString();
    }

    public static boolean isEMailValid(String email) {

        final String EMAIL_PATTERN = "^.+@.+$";
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        return pattern.matcher(email).matches();
    }
}
