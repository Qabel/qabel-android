package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.repository.ContactRepository;
import de.qabel.core.repository.IdentityRepository;
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

}
