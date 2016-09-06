package de.qabel.qabelbox.helper

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.*

@Throws(NumberParseException::class)
fun formatPhoneNumber(phone: String): String {
    val phoneUtil = PhoneNumberUtil.getInstance()
    val parsedPhone = phoneUtil.parse(phone, Locale.getDefault().country)
    return phoneUtil.format(parsedPhone, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
}
