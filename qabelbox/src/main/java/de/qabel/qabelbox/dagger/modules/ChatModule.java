package de.qabel.qabelbox.dagger.modules;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.interactor.ChatUseCase;
import de.qabel.qabelbox.interactor.TransformingChatUseCase;
import de.qabel.qabelbox.transformers.ChatMessageTransformer;
import de.qabel.qabelbox.ui.presenters.ChatPresenter;
import de.qabel.qabelbox.ui.presenters.MainChatPresenter;
import de.qabel.qabelbox.ui.views.ChatView;

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
    ChatMessageTransformer provideTransformer(IdentityRepository identityRepository, ContactRepository contactRepository) {
        return new ChatMessageTransformer(identityRepository, contactRepository);
    }

    @Provides
    public ChatUseCase provideChatUseCase(Identity identity, Contact contact, ChatMessageTransformer transformer, ChatServer chatServer) {
        return new TransformingChatUseCase(identity, contact, transformer, chatServer);
    }

    @ActivityScope
    @Provides
    public ChatPresenter provideChatPresenter(TransformingChatUseCase chatUseCase) {
        return new MainChatPresenter(view, chatUseCase);
    }

    @ActivityScope
    @Provides Contact provideContact(ContactRepository contactRepository, Identity identity) {
        try {
            return contactRepository.findByKeyId(identity, view.getContactKeyId());
        } catch (EntityNotFoundExcepion e) {
            throw new IllegalStateException("Contact not found");
        }
    }
}
