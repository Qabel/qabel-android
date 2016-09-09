package de.qabel.qabelbox.index

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import com.google.i18n.phonenumbers.NumberParseException
import de.qabel.core.index.ExternalContactsAccessor
import de.qabel.core.index.RawContact
import de.qabel.core.logging.QabelLog
import de.qabel.core.logging.debug
import de.qabel.core.logging.info
import de.qabel.qabelbox.helper.Formatter
import de.qabel.qabelbox.helper.formatPhoneNumber

class AndroidContactsAccessor(private val context: Context) : ExternalContactsAccessor, QabelLog {

    private fun Cursor.getString(columnName: String): String = getString(getColumnIndex(columnName))
    private fun Cursor.getInt(columnName: String): Int = getInt(getColumnIndex(columnName))

    override fun getContacts(): List<RawContact> {
        val rawContacts: MutableMap<String, RawContact> = mutableMapOf()
        val contentResolver = context.contentResolver
        contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null).use { contactCursor ->
            if (contactCursor.count > 0) {
                while (contactCursor.moveToNext()) {
                    val id = contactCursor.getString(ContactsContract.Contacts._ID)
                    val name = contactCursor.getString(ContactsContract.Contacts.DISPLAY_NAME)
                    val primaryName = contactCursor.getString(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)

                    val contactName = if (!primaryName.isEmpty()) primaryName else name
                    val hasPhoneNumber = contactCursor.getInt(ContactsContract.Contacts.HAS_PHONE_NUMBER) > 0

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
                        debug("Ignoring contact $contactName without searchable value. ")
                    }
                }
            }
        }
        info("${rawContacts.size} contacts found!")
        return rawContacts.values.sortedBy { it.displayName }
    }

    private fun queryContactEmails(identifier: String): MutableList<String> {
        val resultList = mutableListOf<String>()
        context.contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                arrayOf(identifier), null).use {
            while (it.moveToNext()) {
                val mail = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS))
                if (Formatter.isEMailValid(mail) && !resultList.contains(mail)) {
                    resultList.add(mail)
                }
            }
        }
        return resultList
    }

    private fun queryContactPhones(identifier: String): MutableList<String> {
        val phones = mutableListOf<String>()
        context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(identifier), null).use {
            while (it.moveToNext()) {
                val phoneNo = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
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
