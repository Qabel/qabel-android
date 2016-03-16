package de.qabel.qabelbox.repository;

import java.util.List;

import de.qabel.core.config.Contacts;
import de.qabel.core.config.Persistence;

/**
 * This wrapper is created as in between step, during refactoring.
 * It hold additional functionality used in the android implementaion, which might later be refactored
 * <p/>
 * <p/>
 * Created by Jan D.S. Wischweh <mail@wischweh.de> on 16.03.16.
 */
@Deprecated
public class PeristenceContactRepositoryAndroidWrapper extends PersistenceContactRepositoryDefaultImpl {


    public PeristenceContactRepositoryAndroidWrapper(Persistence<String> persistence) {
        super(persistence);
    }

    public void save(Contacts contacts) {
        persistence.updateEntity(contacts);

    }

    public boolean updateOrPersistEntity(Contacts contacts) {
        Contacts idFromRepo = null;
        if (contacts != null) {
            idFromRepo = persistence.getEntity(contacts.getPersistenceID(), Contacts.class);
        }
        if (idFromRepo != null) {
            return persistence.updateEntity(idFromRepo);
        } else {
            return persistence.persistEntity(contacts);
        }
    }

    public boolean update(Contacts contacts) {
        Contacts idFromRepo = null;
        if (contacts != null) {
            idFromRepo = persistence.getEntity(contacts.getPersistenceID(), Contacts.class);
        }
        if (idFromRepo != null) {
            return persistence.updateEntity(idFromRepo);
        } else {
            return false;
        }
    }

    public List<Contacts> getAll() {
        return persistence.getEntities(Contacts.class);
    }
}
