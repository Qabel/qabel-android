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
        // TODO: persistence.updateOrPersistEntity() ?
        persistence.updateEntity(contacts);
    }


    public List<Contacts> getAll() {
        return persistence.getEntities(Contacts.class);
    }
}
