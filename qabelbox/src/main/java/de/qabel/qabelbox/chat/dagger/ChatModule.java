package de.qabel.qabelbox.chat.dagger;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.exception.EntityNotFoundException;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.chat.interactor.ChatUseCase;
import de.qabel.qabelbox.chat.interactor.TransformingChatUseCase;
import de.qabel.qabelbox.services.DropConnector;
import de.qabel.qabelbox.chat.transformers.ChatMessageTransformer;
import de.qabel.qabelbox.chat.view.presenters.ChatPresenter;
import de.qabel.qabelbox.chat.view.presenters.MainChatPresenter;
import de.qabel.qabelbox.chat.view.views.ChatView;

@ActivityScope
@Module
public class ChatModule {

    private ChatView view;

    public ChatModule(ChatView view) {
        this.view = view;
    }

    @Provides
    public ChatView provideChatView() {
        return view;
    }

    @Provides
    Contact provideContact(ContactRepository contactRepository, Identity identity) {
        try {
            return contactRepository.findByKeyId(identity, view.getContactKeyId());
        } catch (EntityNotFoundException e) {
            throw new IllegalStateException("Contact not found");
        }
    }


    @Provides
    ChatMessageTransformer provideTransformer(IdentityRepository identityRepository,
                                              ContactRepository contactRepository) {
        return new ChatMessageTransformer(identityRepository, contactRepository);
    }


    @Provides
    public ChatUseCase provideChatUseCase(Identity identity, Contact contact,
                                          ChatMessageTransformer transformer, ChatServer chatServer,
                                          DropConnector connector) {
        return new TransformingChatUseCase(identity, contact, transformer, chatServer, connector);
    }


    @Provides ChatServer provideChatServer(Context context) {
        return new ChatServer(context);
    }

    @Provides
    public ChatPresenter provideChatPresenter(ChatUseCase chatUseCase) {
        return new MainChatPresenter(view, chatUseCase);
    }

}
