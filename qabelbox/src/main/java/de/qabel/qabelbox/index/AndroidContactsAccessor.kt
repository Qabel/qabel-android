package de.qabel.qabelbox.index

import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import com.google.i18n.phonenumbers.NumberParseException
import de.qabel.qabelbox.helper.Formatter
import de.qabel.qabelbox.helper.formatPhoneNumber

class AndroidContactsAccessor(private val context: Context) : ExternalContactsAccessor {

    override fun getContacts(): List<RawContact> {
        val rawContacts: MutableMap<String, RawContact> = mutableMapOf()
        val contentResolver = context.contentResolver
        contentResolver.query(Contacts.CONTENT_URI,
                null, null, null, null).use { contactCursor ->
            println("COUNT: " + contactCursor.count)
            if (contactCursor.count > 0) {
                while (contactCursor.moveToNext()) {
                    val id = contactCursor.getString(
                            contactCursor.getColumnIndex(Contacts._ID))
                    val name = contactCursor.getString(contactCursor.getColumnIndex(
                            Contacts.DISPLAY_NAME))
                    val primaryName = contactCursor.getString(contactCursor.getColumnIndex(
                            Contacts.DISPLAY_NAME_PRIMARY))

                    val contactName = if (primaryName?.isEmpty() ?: false) primaryName else name

                    val phones = mutableListOf<String>()
                    if (contactCursor.getInt(contactCursor.getColumnIndex(Contacts.HAS_PHONE_NUMBER)) > 0) {
                        contentResolver.query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + " = ?",
                                arrayOf<String>(id), null).use {
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
                    }

                    val emails = mutableListOf<String>()
                    contentResolver.query(Email.CONTENT_URI, null, Email.CONTACT_ID + " = ?",
                            arrayOf<String>(id), null).use {
                        while (it.moveToNext()) {
                            val mail = it.getString(it.getColumnIndex(Email.ADDRESS))
                            if (Formatter.isEMailValid(mail) && !emails.contains(mail)) {
                                emails.add(mail)
                            }
                        }
                    }
                    if (!emails.isEmpty() || !phones.isEmpty()) {
                        if (rawContacts.containsKey(contactName)) {
                            rawContacts[contactName]?.let {
                                it.emailAddresses.addAll(emails)
                                it.mobilePhoneNumbers.addAll(phones)
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
        println("RESULT COUNT ${rawContacts.size}")
        return rawContacts.values.sortedBy { it.displayName }
    }

}
