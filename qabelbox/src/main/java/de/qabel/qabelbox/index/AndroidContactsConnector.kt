package de.qabel.qabelbox.index

import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts

class AndroidContactsConnector(private val context: Context) : ExternalContactsAccessor {

    override fun getContacts(): List<RawContact> {
        val rawContacts = mutableListOf<RawContact>()
        val contentResolver = context.contentResolver
        contentResolver.query(Contacts.CONTENT_URI,
                null, null, null, null).use { contactCursor ->
            if (contactCursor.count > 0) {
                while (contactCursor.moveToNext()) {
                    val id = contactCursor.getString(
                            contactCursor.getColumnIndex(Contacts._ID))
                    val name = contactCursor.getString(contactCursor.getColumnIndex(
                            Contacts.DISPLAY_NAME))

                    val phones = mutableListOf<String>()
                    if (contactCursor.getInt(contactCursor.getColumnIndex(Contacts.HAS_PHONE_NUMBER)) > 0) {
                        contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                arrayOf<String>(id), null).use {
                            while (it.moveToNext()) {
                                val phoneNo = it.getString(it.getColumnIndex(
                                        ContactsContract.CommonDataKinds.Phone.NUMBER))
                                //TODO normalize with formatter
                                phones.add(phoneNo)
                            }
                        }
                    }

                    val emails = mutableListOf<String>()
                    contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                            arrayOf<String>(id), null).use {
                        while (it.moveToNext()) {
                            val mail = it.getString(it.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Email.ADDRESS))
                            //TODO check/validate
                            emails.add(mail)
                        }
                    }
                    if (!emails.isEmpty() || !phones.isEmpty()) {
                        rawContacts.add(RawContact(name, phones, emails, id))
                    }
                }
            }
        }
        return rawContacts
    }

}
