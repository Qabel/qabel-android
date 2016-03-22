package de.qabel.qabelbox.repository.persistence;

import android.support.annotation.NonNull;

import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.config.Persistence;
import de.qabel.qabelbox.repository.DeletableIdentityRepository;
import de.qabel.qabelbox.repository.exception.EntityNotFoundExcepion;
import de.qabel.qabelbox.repository.exception.PersistenceException;

/**
 * Created by Jan D.S. Wischweh <mail@wischweh.de> on 16.03.16.
 */
@Deprecated
public class IdentityRepositoryDeleteableImplementation extends PersistenceIdentityRepositoryImpl implements DeletableIdentityRepository {
    private static final String TAG = "PersistenceIdentity";

    public IdentityRepositoryDeleteableImplementation(Persistence<String> persistence) {
        super(persistence);
    }


    @Override
    public void delete(@NonNull Identity identity) throws PersistenceException, EntityNotFoundExcepion {
        Identities identities = findAll();
        if (!identities.contains(identity)) {
            throw new EntityNotFoundExcepion("Could not find entity " + identity.getAlias());
        }
        identities.remove(identity);
        persistence.updateEntity(identities);
    }
}
