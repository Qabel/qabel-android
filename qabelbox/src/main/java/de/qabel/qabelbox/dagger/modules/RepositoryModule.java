package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.repository.ChatDropMessageRepository;
import de.qabel.core.repository.ContactRepository;
import de.qabel.core.repository.DropStateRepository;
import de.qabel.core.repository.IdentityRepository;
import de.qabel.core.repository.sqlite.SqliteChatDropMessageRepository;
import de.qabel.core.repository.sqlite.SqliteDropStateRepository;
import de.qabel.core.repositories.AndroidClientDatabase;
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

    @Singleton @Provides DropStateRepository provideDropStateRepository(
            RepositoryFactory factory, AndroidClientDatabase database) {
        return new SqliteDropStateRepository(database, factory.getEntityManager());
    }

    @Singleton @Provides ChatDropMessageRepository provideChatDropMessageRepository(
            RepositoryFactory factory, AndroidClientDatabase database) {
        return new SqliteChatDropMessageRepository(database, factory.getEntityManager());
    }

}
