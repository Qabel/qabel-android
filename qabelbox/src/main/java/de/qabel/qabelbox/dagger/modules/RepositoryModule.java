package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.desktop.repository.sqlite.AndroidClientDatabase;
import de.qabel.qabelbox.persistence.RepositoryFactory;

@Module
public class RepositoryModule {

    @Provides AndroidClientDatabase provideAndroidClientDatabase(RepositoryFactory factory) {
        return factory.getAndroidClientDatabase();
    }

    @Provides RepositoryFactory provideRepositoryFactory(Context context) {
        return new RepositoryFactory(context);
    }

    @Provides IdentityRepository provideIdentityRepository(
            RepositoryFactory factory, AndroidClientDatabase database) {
        return factory.getIdentityRepository(database);
    }

    @Provides ContactRepository provideContactRepository(
            RepositoryFactory factory, AndroidClientDatabase database) {
        return factory.getContactRepository(database);
    }

    @Provides Identities provideIdentities(IdentityRepository repository) {
        try {
            return repository.findAll();
        } catch (PersistenceException e) {
            throw new IllegalStateException("Could not retrieve Identities", e);
        }
    }

    @Provides Map<Identity, Contacts> provideContacts(ContactRepository repository, Identities identities) {
        try {
            Map<Identity, Contacts> contacts = new HashMap<>();
            for (Identity identity: identities.getIdentities()) {
                contacts.put(identity, repository.find(identity));
            }
            return contacts;
        } catch (PersistenceException e) {
            throw new IllegalStateException("Could not retrieve Identities", e);
        }
    }
}
