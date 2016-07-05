package de.qabel.qabelbox.contacts

import de.qabel.core.config.Contact
import org.hamcrest.Description
import org.hamcrest.TypeSafeDiagnosingMatcher

class ContactMatcher(private val contact: Contact) : TypeSafeDiagnosingMatcher<Contact>() {

    override fun matchesSafely(item: Contact?, mismatchDescription: Description?): Boolean {
        if (item == null) {
            return false;
        }
        val matches = contact.alias.equals(item.alias);
        matches.and(contact.keyIdentifier.equals(item.keyIdentifier));
        matches.and(contact.email?.equals(item.email) ?: item.email == null);
        matches.and(contact.phone?.equals(item.phone) ?: item.phone == null);
        matches.and(contact.dropUrls.equals(item.dropUrls));
        return matches;
    }


    override fun describeTo(description: Description?) {
        description?.appendText("Matching with contact")
                ?.appendText(contact.alias)
                ?.appendText("(")
                ?.appendText(contact.keyIdentifier)
                ?.appendText(")")
    }

}
