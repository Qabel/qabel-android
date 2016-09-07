package de.qabel.qabelbox.index

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Contacts.*
import com.google.i18n.phonenumbers.NumberParseException
import de.qabel.qabelbox.helper.Formatter
import de.qabel.qabelbox.helper.formatPhoneNumber

class AndroidContactsAccessor(private val context: Context) : ExternalContactsAccessor {

    private fun Cursor.getString(columnName: String): String = getString(getColumnIndex(columnName))
    private fun Cursor.getInt(columnName: String): Int = getInt(getColumnIndex(columnName))

    override fun getContacts(): List<RawContact> {
        val rawContacts: MutableMap<String, RawContact> = mutableMapOf()
        val contentResolver = context.contentResolver
        contentResolver.query(CONTENT_URI,
                null, null, null, null).use { contactCursor ->
            if (contactCursor.count > 0) {
                while (contactCursor.moveToNext()) {
                    val id = contactCursor.getString(_ID)
                    val name = contactCursor.getString(DISPLAY_NAME)
                    val primaryName = contactCursor.getString(DISPLAY_NAME_PRIMARY)

                    val contactName = if (!primaryName.isEmpty()) primaryName else name
                    val hasPhoneNumber = contactCursor.getInt(HAS_PHONE_NUMBER) > 0

                    val phones: MutableList<String> = if (hasPhoneNumber) queryContactPhones(id) else mutableListOf()
                    val emails: MutableList<String> = queryContactEmails(id)

                    if (!emails.isEmpty() || !phones.isEmpty()) {
                        if (rawContacts.containsKey(contactName)) {
                            rawContacts[contactName]?.let { rawContact ->
                                rawContact.emailAddresses.addAll(emails.filter { rawContact.emailAddresses.contains(it) })
                                rawContact.mobilePhoneNumbers.addAll(phones.filter { rawContact.mobilePhoneNumbers.contains(it) })
                            }
                        } else {
                            rawContacts.put(contactName, RawContact(contactName, phones, emails, id))
                        }
                    } else {
                        println("ignoring contact $contactName")
                    }
                }
            }
        }
        val allValues = rawContacts.values.flatMap {
            mutableListOf<String>().apply {
                addAll(it.emailAddresses)
                addAll(it.mobilePhoneNumbers)
            }
        }
        println("RESULT COUNT ${rawContacts.size} ${allValues.size}")
        return rawContacts.values.sortedBy { it.displayName }
    }

    private fun queryContactEmails(identifier: String): MutableList<String> {
        val resultList = mutableListOf<String>()
        context.contentResolver.query(Email.CONTENT_URI, null, Email.CONTACT_ID + " = ?",
                arrayOf(identifier), null).use {
            while (it.moveToNext()) {
                val mail = it.getString(it.getColumnIndex(Email.ADDRESS))
                if (Formatter.isEMailValid(mail) && !resultList.contains(mail)) {
                    resultList.add(mail)
                }
            }
        }
        return resultList
    }

    private fun queryContactPhones(identifier: String): MutableList<String> {
        val phones = mutableListOf<String>()
        context.contentResolver.query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + " = ?",
                arrayOf(identifier), null).use {
            while (it.moveToNext()) {
                val phoneNo = it.getString(it.getColumnIndex(Phone.NUMBER))
                if (!phoneNo.isEmpty()) {
                    try {
                        val normalized = formatPhoneNumber(phoneNo)
                        if (!phones.contains(normalized)) {
                            phones.add(normalized)
                        }
                    } catch (ex: NumberParseException) {
                        //Ignore invalid numbers
                    }
                }
            }
        }
        return phones
    }

}
