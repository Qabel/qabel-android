package de.qabel.qabelbox.dagger.modules;

import android.content.Context;
import android.support.v7.app.NotificationCompat;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.internal.Factory;
import de.qabel.chat.repository.ChatDropMessageRepository;
import de.qabel.chat.service.ChatService;
import de.qabel.chat.service.MainChatService;
import de.qabel.chat.service.SharingService;
import de.qabel.core.http.MainDropConnector;
import de.qabel.core.http.MainDropServer;
import de.qabel.core.repository.ContactRepository;
import de.qabel.core.repository.DropStateRepository;
import de.qabel.core.repository.IdentityRepository;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.chat.interactor.ChatServiceUseCase;
import de.qabel.qabelbox.chat.interactor.IntentMessageStateBroadcaster;
import de.qabel.qabelbox.chat.interactor.MainChatServiceUseCase;
import de.qabel.qabelbox.chat.interactor.MainMarkAsRead;
import de.qabel.qabelbox.chat.interactor.MarkAsRead;
import de.qabel.qabelbox.chat.interactor.MessageStateBroadcaster;
import de.qabel.qabelbox.chat.notifications.ChatNotificationManager;
import de.qabel.qabelbox.chat.notifications.MainChatNotificationManager;
import de.qabel.qabelbox.chat.notifications.presenter.AndroidChatNotificationPresenter;
import de.qabel.qabelbox.chat.notifications.presenter.ChatNotificationPresenter;
import de.qabel.qabelbox.contacts.interactor.ReadOnlyContactsInteractor;
import de.qabel.qabelbox.contacts.interactor.RepositoryReadOnlyContactsInteractor;
import de.qabel.qabelbox.identity.interactor.IdentityInteractor;
import de.qabel.qabelbox.identity.interactor.MainIdentityInteractor;
import de.qabel.qabelbox.identity.interactor.RepositoryReadOnlyIdentityInteractor;
import de.qabel.qabelbox.identity.interactor.ReadOnlyIdentityInteractor;
import de.qabel.qabelbox.listeners.ActionIntentSender;
import de.qabel.qabelbox.listeners.AndroidActionIntentCastSender;

@Module
public class ApplicationModule extends ContextModule {

    private QabelBoxApplication application;

    public ApplicationModule(QabelBoxApplication application) {
        super(application);
        this.application = application;
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
    MessageStateBroadcaster providesSendMessagesReadEvent(IntentMessageStateBroadcaster intentSendMessagesReadEvent) {
        return intentSendMessagesReadEvent;
    }

    @Singleton
    @Provides
    ChatServiceUseCase providesChatManager(MainChatServiceUseCase mainChatServiceUseCase) {
        return mainChatServiceUseCase;
    }

    @Singleton
    @Provides
    MarkAsRead provideMarkAsRead(MainMarkAsRead mainMarkAsRead) {
        return mainMarkAsRead;
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
    public IdentityInteractor providesIdentityUseCase(MainIdentityInteractor useCase) {
        return useCase;
    }

    @Provides
    public ReadOnlyIdentityInteractor providesReadOnlyIdentityInteractor(
            RepositoryReadOnlyIdentityInteractor interactor) {
        return interactor;
    }

    @Provides
    public ReadOnlyContactsInteractor providesReadOnlyContactsInteractor(
            RepositoryReadOnlyContactsInteractor interactor) {
        return interactor;
    }

}
