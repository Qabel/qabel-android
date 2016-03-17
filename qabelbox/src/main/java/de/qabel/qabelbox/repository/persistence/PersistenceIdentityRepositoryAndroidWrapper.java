package de.qabel.qabelbox.repository.persistence;

import de.qabel.core.config.Identities;
import de.qabel.core.config.Persistence;

/**
 * Created by Jan D.S. Wischweh <mail@wischweh.de> on 16.03.16.
 */
@Deprecated
public class PersistenceIdentityRepositoryAndroidWrapper extends PersistenceIdentityRepositoryDefaultImpl {
    private static final String TAG = "PersistenceIdentity";

    public PersistenceIdentityRepositoryAndroidWrapper(Persistence<String> persistence) {
        super(persistence);
    }


    public boolean update(Identities identity) {
        Identities idFromRepo = null;
        if (identity != null) {
            idFromRepo = persistence.getEntity(identity.getPersistenceID(), Identities.class);
        }
        if (idFromRepo != null) {
            return persistence.updateEntity(idFromRepo);
        } else {
            return false;
        }
    }
}
