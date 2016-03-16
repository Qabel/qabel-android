package de.qabel.qabelbox.repository;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.repository.exception.EntityNotFoundExcepion;
import de.qabel.qabelbox.repository.exception.PersistenceException;

public interface ContactRepository {

    Contacts find(Identity identity);

    void save(Contact contact, Identity identity) throws PersistenceException;

    void delete(Contact contact, Identity identity) throws PersistenceException;

    Contact findByKeyId(Identity identity, String keyId) throws EntityNotFoundExcepion;


}
