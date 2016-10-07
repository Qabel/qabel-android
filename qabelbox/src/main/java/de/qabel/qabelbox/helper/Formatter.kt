package de.qabel.qabelbox.helper

import android.content.Context
import android.content.res.Resources
import android.text.format.DateUtils

import java.text.DateFormat
import java.text.DecimalFormat
import java.util.Date
import java.util.regex.Pattern

import de.qabel.qabelbox.R

object Formatter {

    private val dateFormat by lazy { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    private val EMAIL_PATTERN by lazy { Pattern.compile("^.+@.+$") }

    @JvmStatic
    fun formatDateShort(date: Date): String =
            dateFormat.format(date)

    /**
     * Formats the diff of a Date[time] with the current system time to a String
     * like "moments ago" if [time] is less than a minutes in the past or
     * "Fr. 20:35" or "15.02.15 20:35"
     * if the [time] is more than a week in the past.
     */
    fun formatDateTimeString(time: Long, ctx: Context): String {
        val diffMinutes = (System.currentTimeMillis() - time) / DateUtils.MINUTE_IN_MILLIS
        if (diffMinutes == 0L) {
            return ctx.getString(R.string.moments_ago)
        }
        return DateUtils.getRelativeTimeSpanString(time).toString()
    }

    fun isEMailValid(email: String): Boolean = EMAIL_PATTERN.matcher(email).matches()
}
