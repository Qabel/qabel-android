package de.qabel.qabelbox.helper;

import de.qabel.core.config.Contact;

/**
 * Created by danny on 11.02.16.
 */
public class Helper {

    public static final String INTENT_REFRESH_CONTACTLIST = "de.qabel.qabelbox.refreshContactList";

    public static String getDropIdFromContact(Contact contact) {

        String[] elements = contact.getDropUrls().iterator().next().toString().split("/");
        return elements[elements.length - 1];
    }
}
