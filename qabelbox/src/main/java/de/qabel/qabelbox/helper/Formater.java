package de.qabel.qabelbox.helper;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;

/**
 * Created by danny on 14.01.2016.
 */
public class Formater {

    private static final long KB = 1024;
    private static final long MB = 1024 * 1024;
    private static final long GB = 1024 * 1024 * 1024;

    private static final DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    private static final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    public static String formatFileSizeHumanReadable(long filesize) {
        String result = "";
        DecimalFormat df = new DecimalFormat("#.##");
        if (filesize < KB) {
            result = filesize + " Bytes";
        } else if (filesize < MB)
            result = df.format(filesize / KB) + " KB";
        else if (filesize < GB)
            result = df.format(filesize / MB) + " MB";
        else {
            result = df.format(filesize / GB) + "  GB";
        }
        return result;
    }

    private static String formatDateShort(Date date) {
        return dateFormat.format(date);
    }

    public static String formatDateShort(long date) {
        return formatDateShort(new Date(date));
    }

    private static String formatDateTimeShort(Date date) {
        return dateFormat.format(date);
    }

    public static String formatDateTimeShort(long date) {
        return formatDateTimeShort(new Date(date));
    }
}
