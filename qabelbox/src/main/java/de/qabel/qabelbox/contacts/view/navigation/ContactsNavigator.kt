package de.qabel.qabelbox.contacts.view.navigation

import android.app.Activity
import de.qabel.core.config.Contact


interface ContactsNavigator {

    fun showQrCodeFragment(activity: Activity, contact: Contact)

}
