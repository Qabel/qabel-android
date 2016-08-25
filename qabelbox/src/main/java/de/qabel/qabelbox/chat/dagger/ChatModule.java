package de.qabel.qabelbox.chat.dagger;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.repository.ContactRepository;
import de.qabel.core.repository.IdentityRepository;
import de.qabel.core.repository.exception.EntityNotFoundException;
import de.qabel.qabelbox.chat.interactor.ChatUseCase;
import de.qabel.qabelbox.chat.interactor.TransformingChatUseCase;
import de.qabel.qabelbox.chat.transformers.ChatMessageTransformer;
import de.qabel.qabelbox.chat.view.presenters.ChatPresenter;
import de.qabel.qabelbox.chat.view.presenters.MainChatPresenter;
import de.qabel.qabelbox.chat.view.views.ChatView;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;

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
    public ChatUseCase provideChatUseCase(TransformingChatUseCase useCase) {
        return useCase;
    }

    @Provides
    public ChatPresenter provideChatPresenter(MainChatPresenter presenter) {
        return presenter;
    }

}
