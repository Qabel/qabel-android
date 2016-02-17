package de.qabel.qabelbox.helper;

import android.content.Context;
import android.content.res.Resources;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.regex.Pattern;

import de.qabel.qabelbox.R;

/**
 * Created by danny on 14.01.2016.
 */
public class Formatter {

    private static final long KB = 1024;
    private static final long MB = 1024 * 1024;
    private static final long GB = 1024 * 1024 * 1024;

    private static final DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    private static final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

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

    public static boolean isEMailValid(String email) {

        final String EMAIL_PATTERN = "^.+@.+$";
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        return pattern.matcher(email).matches();
    }
}
