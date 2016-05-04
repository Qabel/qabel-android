package de.qabel.qabelbox.persistence;

import java.util.List;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identity;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion;
import de.qabel.desktop.repository.exception.PersistenceException;

public class PersistenceMigrator {

    public static void migrate(AndroidPersistence persistence,
                               IdentityRepository identityRepository,
                               ContactRepository contactRepository)
            throws PersistenceException, EntityNotFoundExcepion {
        List<Identity> identities = persistence.getEntities(Identity.class);
        List<Contacts> contactsList = persistence.getEntities(Contacts.class);
        for (Identity identity: identities) {
            identityRepository.save(identity);
        }
        for (Contacts contacts: contactsList) {
            for (Contact contact: contacts.getContacts()) {
                Identity identity = identityRepository.find(
                        contacts.getIdentity().getKeyIdentifier());
                contactRepository.save(contact, identity);
            }
        }

    }
}
