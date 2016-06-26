package de.qabel.desktop.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion;
import de.qabel.desktop.repository.exception.PersistenceException;
import kotlin.Pair;

public interface ContactRepository {

    Contacts find(Identity identity) throws PersistenceException;

    void save(Contact contact, Identity identity) throws PersistenceException;

    void delete(Contact contact, Identity identity) throws PersistenceException, EntityNotFoundExcepion;

    Contact findByKeyId(Identity identity, String keyId) throws EntityNotFoundExcepion;

    Collection<Pair<Contact, List<Identity>>> findWithIdentities(String searchString) throws PersistenceException;

    Contact findByKeyId(String keyId) throws EntityNotFoundExcepion;

    List<Identity> findContactIdentities(String contactKey) throws PersistenceException, EntityNotFoundExcepion;
}
