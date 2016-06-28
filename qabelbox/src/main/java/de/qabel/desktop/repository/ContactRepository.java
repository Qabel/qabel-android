package de.qabel.desktop.repository;

import java.util.Collection;
import java.util.List;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identity;
import de.qabel.desktop.repository.exception.EntityNotFoundException;
import de.qabel.desktop.repository.exception.PersistenceException;
import kotlin.Pair;

public interface ContactRepository {

    Contacts find(Identity identity) throws PersistenceException;

    void save(Contact contact, Identity identity) throws PersistenceException;

    void delete(Contact contact, Identity identity) throws PersistenceException, EntityNotFoundException;

    Contact findByKeyId(Identity identity, String keyId) throws EntityNotFoundException;

    Contact findByKeyId(String keyId) throws EntityNotFoundException;

    Boolean exists(Contact contact) throws PersistenceException;

    Pair<Contact, List<Identity>> findContactWithIdentities(String key) throws PersistenceException, EntityNotFoundException;

    Collection<Pair<Contact, List<Identity>>> findWithIdentities(String searchString) throws PersistenceException;

}
