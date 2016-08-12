package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.repository.ChatDropMessageRepository;
import de.qabel.core.repository.ContactRepository;
import de.qabel.core.repository.DropStateRepository;
import de.qabel.core.repository.IdentityRepository;
import de.qabel.qabelbox.persistence.RepositoryFactory;

@Module
public class RepositoryModule {

    @Singleton
    @Provides
    RepositoryFactory provideRepositoryFactory(Context context) {
        return new RepositoryFactory(context);
    }

    @Provides
    IdentityRepository provideIdentityRepository(
            RepositoryFactory factory) {
        return factory.getIdentityRepository();
    }

    @Provides
    ContactRepository provideContactRepository(
            RepositoryFactory factory) {
        return factory.getContactRepository();
    }

    @Provides
    DropStateRepository provideDropStateRepository(
            RepositoryFactory factory) {
        return factory.getDropStateRepository();
    }

    @Provides
    ChatDropMessageRepository provideChatDropMessageRepository(
            RepositoryFactory factory) {
        return factory.getChatDropMessageRepository();
    }

}
