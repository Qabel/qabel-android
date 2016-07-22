package de.qabel.qabelbox.navigation

import android.app.Fragment
import android.content.Intent
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.IdentityRepository
import de.qabel.core.repository.exception.EntityNotFoundException
import de.qabel.core.repository.exception.PersistenceException
import de.qabel.qabelbox.R
import de.qabel.qabelbox.activities.CreateAccountActivity
import de.qabel.qabelbox.activities.MainActivity
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.view.views.ContactDetailsFragment
import de.qabel.qabelbox.contacts.view.views.ContactsFragment
import de.qabel.qabelbox.fragments.AboutLicencesFragment
import de.qabel.qabelbox.fragments.HelpMainFragment
import de.qabel.qabelbox.fragments.IdentitiesFragment
import de.qabel.qabelbox.fragments.QRcodeFragment
import de.qabel.qabelbox.chat.view.views.ChatFragment
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.warn
import javax.inject.Inject

class MainNavigator
@Inject
constructor(var activity: MainActivity,
            var identityRepository: IdentityRepository,
            var contactRepository: ContactRepository,
            var activeIdentity: Identity) : AbstractNavigator(), Navigator, AnkoLogger {

    companion object {
        const val TAG_CONTACT_LIST_FRAGMENT = "TAG_CONTACT_LIST_FRAGMENT"
        const val TAG_CONTACT_CHAT_FRAGMENT = "TAG_CONTACT_CHAT_FRAGMENT"
        const val TAG_CONTACT_DETAILS_FRAGMENT = "TAG_CONTACT_DETAILS_FRAGMENT";
        const val TAG_QR_CODE_FRAGMENT = "TAG_CONTACT_QR_CODE_FRAGMENT";

        const val TAG_FILES_FRAGMENT = "TAG_FILES_FRAGMENT"
        const val TAG_ABOUT_FRAGMENT = "TAG_ABOUT_FRAGMENT"
        const val TAG_HELP_FRAGMENT = "TAG_HELP_FRAGMENT"
        const val TAG_MANAGE_IDENTITIES_FRAGMENT = "TAG_MANAGE_IDENTITIES_FRAGMENT"
        const val TAG_FILES_SHARE_INTO_APP_FRAGMENT = "TAG_FILES_SHARE_INTO_APP_FRAGMENT"

    }

    private fun showMainFragment(fragment: Fragment, tag: String) {
        showFragment(activity, fragment, tag, false, true)
        activity.handleMainFragmentChange()
    }

    override fun selectCreateAccountActivity() {
        val intent = Intent(activity, CreateAccountActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
        activity.startActivity(intent)
        activity.finish()
    }

    /*
        FRAGMENT SELECTION METHODS
    */
    override fun selectManageIdentitiesFragment() {
        try {
            showMainFragment(IdentitiesFragment.newInstance(identityRepository.findAll()),
                    TAG_MANAGE_IDENTITIES_FRAGMENT)
        } catch (e: PersistenceException) {
            throw RuntimeException(e)
        }

    }

    override fun selectHelpFragment() {
        showMainFragment(HelpMainFragment(), TAG_HELP_FRAGMENT)
    }

    override fun selectAboutFragment() {
        showMainFragment(AboutLicencesFragment(), TAG_ABOUT_FRAGMENT)
    }

    override fun selectFilesFragment() {
        TODO("FilesFragment is under construction");
    }

    override fun selectContactsFragment() {
        showMainFragment(ContactsFragment(), TAG_CONTACT_LIST_FRAGMENT)
    }

    override fun selectQrCodeFragment(contact: Contact) {
        showFragment(activity, QRcodeFragment.newInstance(contact), TAG_QR_CODE_FRAGMENT, true, false);
    }

    override fun selectContactDetailsFragment(contactDto: ContactDto) {
        showFragment(activity, ContactDetailsFragment.withContact(contactDto), TAG_CONTACT_DETAILS_FRAGMENT, true, false);
    }

    override fun selectChatFragment(activeContact: String?) {
        if (activeContact == null) {
            return
        }
        selectContactChat(activeContact, activeIdentity);
    }

    override fun selectContactChat(contactKey: String, withIdentity: Identity) {
        if (activeIdentity.keyIdentifier.equals(withIdentity.keyIdentifier)) {
            try {
                val contact = contactRepository.findByKeyId(withIdentity, contactKey);
                showFragment(activity, ChatFragment.withContact(contact), TAG_CONTACT_CHAT_FRAGMENT, true, true)
            } catch (entityNotFoundException: EntityNotFoundException) {
                warn("Could not find contact " + contactKey, entityNotFoundException)
            }
        } else {
            val intent = Intent(activity, MainActivity::class.java)
            intent.putExtra(MainActivity.START_CONTACTS_FRAGMENT, true);
            intent.putExtra(MainActivity.ACTIVE_IDENTITY, withIdentity.keyIdentifier)
            intent.putExtra(MainActivity.ACTIVE_CONTACT, contactKey)
            activity.finish()
            activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            activity.startActivity(intent)
        }
    }

}
