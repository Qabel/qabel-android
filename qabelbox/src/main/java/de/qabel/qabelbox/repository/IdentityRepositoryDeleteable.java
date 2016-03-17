package de.qabel.qabelbox.repository;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.repository.exception.EntityNotFoundExcepion;
import de.qabel.qabelbox.repository.exception.PersistenceException;

/**
 * Created by Jan D.S. Wischweh <mail@wischweh.de> on 17.03.16.
 */
public interface IdentityRepositoryDeleteable extends IdentityRepository {

    void delete(Identity identity) throws PersistenceException, EntityNotFoundExcepion;
}
