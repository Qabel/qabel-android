package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.qabel.box.storage.local.BoxLocalStorage;
import de.qabel.box.storage.local.LocalStorage;
import de.qabel.box.storage.local.repository.LocalStorageRepository;
import de.qabel.chat.repository.ChatDropMessageRepository;
import de.qabel.chat.repository.ChatShareRepository;
import de.qabel.core.crypto.CryptoUtils;
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

    @Singleton @Provides
    IdentityRepository provideIdentityRepository(
            RepositoryFactory factory) {
        return factory.getIdentityRepository();
    }

    @Singleton @Provides
    ContactRepository provideContactRepository(
            RepositoryFactory factory) {
        return factory.getContactRepository();
    }

    @Singleton @Provides
    DropStateRepository provideDropStateRepository(
            RepositoryFactory factory) {
        return factory.getDropStateRepository();
    }

    @Singleton @Provides
    ChatDropMessageRepository provideChatDropMessageRepository(
            RepositoryFactory factory) {
        return factory.getChatDropMessageRepository();
    }

    @Singleton @Provides
    ChatShareRepository providesChatShareRepository(RepositoryFactory factory){
        return factory.getChatShareRepository();
    }

    @Singleton
    @Provides
    LocalStorageRepository providesLocalStorageRepo(RepositoryFactory factory) {
        return factory.getLocalStorageRepository();
    }

    @Singleton
    @Provides
    LocalStorage providesLocalStorage(Context context, LocalStorageRepository localStorageRepo) {
        return new BoxLocalStorage(new File(context.getCacheDir(), "local_storage"), context.getExternalCacheDir(), new CryptoUtils(), localStorageRepo);
    }
}
