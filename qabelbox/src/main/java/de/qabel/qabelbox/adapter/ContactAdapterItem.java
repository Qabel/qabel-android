package de.qabel.qabelbox.adapter;

import de.qabel.core.config.Contact;

public class ContactAdapterItem extends Contact {
    boolean hasNewMessages = false;

    public ContactAdapterItem(Contact contact, boolean hasNewMessages) {
        super(contact.getAlias(), contact.getDropUrls(), contact.getEcPublicKey());
        setEmail((contact.getEmail()));
        setPhone((contact.getPhone()));
        setId(contact.getId());
        this.hasNewMessages = hasNewMessages;
    }
}
