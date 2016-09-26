package de.qabel.qabelbox.dagger.modules;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.internal.Factory;
import de.qabel.box.storage.StorageReadBackend;
import de.qabel.chat.repository.ChatDropMessageRepository;
import de.qabel.chat.repository.ChatShareRepository;
import de.qabel.chat.service.ChatService;
import de.qabel.chat.service.MainChatService;
import de.qabel.chat.service.MainSharingService;
import de.qabel.chat.service.SharingService;
import de.qabel.core.config.Identities;
import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.http.MainDropConnector;
import de.qabel.core.http.MainDropServer;
import de.qabel.core.repository.ContactRepository;
import de.qabel.core.repository.DropStateRepository;
import de.qabel.core.repository.IdentityRepository;
import de.qabel.core.repository.exception.PersistenceException;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.box.backends.BoxHttpStorageBackend;
import de.qabel.qabelbox.chat.notifications.presenter.AndroidChatNotificationPresenter;
import de.qabel.qabelbox.chat.notifications.ChatNotificationManager;
import de.qabel.qabelbox.chat.notifications.presenter.ChatNotificationPresenter;
import de.qabel.qabelbox.chat.notifications.MainChatNotificationManager;
import de.qabel.qabelbox.chat.interactor.ChatServiceUseCase;
import de.qabel.qabelbox.chat.interactor.MainChatServiceUseCase;
import de.qabel.qabelbox.chat.transformers.ChatMessageTransformer;
import de.qabel.qabelbox.identity.interactor.IdentityUseCase;
import de.qabel.qabelbox.identity.interactor.MainIdentityUseCase;
import de.qabel.qabelbox.listeners.ActionIntentSender;
import de.qabel.qabelbox.listeners.AndroidActionIntentCastSender;
import de.qabel.qabelbox.storage.server.BlockServer;

@Module
public class ApplicationModule extends ContextModule {

    public ApplicationModule(QabelBoxApplication application) {
        super(application);
    }

    @Provides
    ActionIntentSender providesActionIntentSender(Context context) {
        return new AndroidActionIntentCastSender(context);
    }

    @Singleton
    @Provides
    ChatNotificationManager providesChatNotificationManager(
            MainChatNotificationManager manager) {
        return manager;
    }

    @Singleton
    @Provides
    ChatNotificationPresenter providesChatNotificationPresenter(
            AndroidChatNotificationPresenter presenter) {
        return presenter;
    }

    @Singleton
    @Provides
    ChatService providesChatService(IdentityRepository identityRepository, ContactRepository contactRepository,
                                    DropStateRepository dropStateRepository, SharingService sharingService,
                                    ChatDropMessageRepository chatDropMessageRepository) {
        return new MainChatService(new MainDropConnector(new MainDropServer()), identityRepository,
                contactRepository, chatDropMessageRepository, dropStateRepository, sharingService);
    }


    @Singleton
    @Provides
    ChatServiceUseCase providesChatManager(ContactRepository contactRepo, ChatDropMessageRepository chatDropMessageRepository,
                                           IdentityRepository identityRepo, ChatMessageTransformer msgTransformer) {
        return new MainChatServiceUseCase(chatDropMessageRepository, contactRepo, identityRepo, msgTransformer);
    }

    @Provides
    Factory<NotificationCompat.Builder> providesNotificationBuilder(
            final Context context) {
        return new Factory<NotificationCompat.Builder>() {
            @Override
            public NotificationCompat.Builder get() {
                return new NotificationCompat.Builder(context);
            }
        };
    }


    @Provides
    public IdentityUseCase providesIdentityUseCase(MainIdentityUseCase useCase) {
        return useCase;
    }

}
