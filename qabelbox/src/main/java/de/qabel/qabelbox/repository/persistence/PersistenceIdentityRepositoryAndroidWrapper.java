package de.qabel.qabelbox.repository.persistence;

import android.util.Log;

import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
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

    public PersistenceIdentityRepositoryAndroidWrapper(AndroidPersistence androidPersistence) {
        super(androidPersistence);
    }


    public boolean updateOrPersistEntity(Identity identity) {
        Identity idFromRepo = null;
        if (identity != null) {
            idFromRepo = persistence.getEntity(identity.getPersistenceID(), Identity.class);
        }
        if (idFromRepo != null) {
            return persistence.updateEntity(idFromRepo);
        } else {
            return persistence.persistEntity(identity);
        }
    }

    public void delete(Identity identity) {
        if (identity != null) {
            persistence.removeEntity(identity.getPersistenceID(), Identity.class);
        } else {
            Log.w(TAG, "delete(null): Will ignore call to delte null object");
        }
    }

    public boolean update(Identity identity) {
        Identity idFromRepo = null;
        if (identity != null) {
            idFromRepo = persistence.getEntity(identity.getPersistenceID(), Identity.class);
        }
        if (idFromRepo != null) {
            return persistence.updateEntity(idFromRepo);
        } else {
            return false;
        }
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
