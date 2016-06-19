package de.qabel.qabelbox.contacts.navigation

import android.app.Activity
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.qabelbox.fragments.QRcodeFragment
import de.qabel.qabelbox.navigation.AbstractNavigator
import javax.inject.Inject


class MainContactsNavigator @Inject constructor(private val identity: Identity) : ContactsNavigator, AbstractNavigator() {

    private val TAG_QR_CODE_FRAGMENT = "TAG_CONTACT_QR_CODE_FRAGMENT";

    override fun showQrCodeFragment(activity: Activity, contact: Contact) {
        showFragment(activity, QRcodeFragment.newInstance(contact), TAG_QR_CODE_FRAGMENT, true, false);
    }

}
