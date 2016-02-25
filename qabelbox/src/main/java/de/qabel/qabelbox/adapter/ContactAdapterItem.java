package de.qabel.qabelbox.adapter;

import de.qabel.core.config.Contact;

/**
 * Created by danny on 25.02.16.
 */
public class ContactAdapterItem extends Contact {
	boolean hasNewMessages = false;

	public ContactAdapterItem(Contact contact, boolean hasNewMessages) {
		setAlias((contact.getAlias()));
		setEcPublicKey((contact.getEcPublicKey()));
		setEmail((contact.getEmail()));
		setPhone((contact.getPhone()));
		this.hasNewMessages = hasNewMessages;
	}
}
