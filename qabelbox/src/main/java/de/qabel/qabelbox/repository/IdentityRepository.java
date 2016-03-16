package de.qabel.qabelbox.repository;

import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.repository.exception.EntityNotFoundExcepion;
import de.qabel.qabelbox.repository.exception.PersistenceException;

public interface IdentityRepository {
    /**
     * @param id KeyIdentifier of the Identities public key
     * @throws EntityNotFoundExcepion
     * @throws PersistenceException
     */
    Identity find(String id) throws EntityNotFoundExcepion, PersistenceException;

    Identities findAll() throws EntityNotFoundExcepion, PersistenceException;

    void save(Identity identity) throws PersistenceException;
}
