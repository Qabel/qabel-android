package de.qabel.qabelbox.chat.dagger;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.repository.ContactRepository;
import de.qabel.core.repository.IdentityRepository;
import de.qabel.core.repository.exception.EntityNotFoundException;
import de.qabel.qabelbox.chat.interactor.ChatOverviewUseCase;
import de.qabel.qabelbox.chat.interactor.ChatUseCase;
import de.qabel.qabelbox.chat.interactor.MainChatOverviewUseCase;
import de.qabel.qabelbox.chat.interactor.TransformingChatUseCase;
import de.qabel.qabelbox.chat.transformers.ChatMessageTransformer;
import de.qabel.qabelbox.chat.view.presenters.ChatOverviewPresenter;
import de.qabel.qabelbox.chat.view.presenters.ChatPresenter;
import de.qabel.qabelbox.chat.view.presenters.MainChatOverviewPresenter;
import de.qabel.qabelbox.chat.view.presenters.MainChatPresenter;
import de.qabel.qabelbox.chat.view.views.ChatOverview;
import de.qabel.qabelbox.chat.view.views.ChatView;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;

@ActivityScope
@Module
public class ChatOverviewModule {

    private ChatOverview view;

    public ChatOverviewModule(ChatOverview view) {
        this.view = view;
    }

    @Provides
    public ChatOverview provideChatOverview() {
        return view;
    }

    @Provides
    ChatMessageTransformer provideTransformer(IdentityRepository identityRepository,
                                              ContactRepository contactRepository) {
        return new ChatMessageTransformer(identityRepository, contactRepository);
    }

    @Provides
    public ChatOverviewUseCase provideChatUseCase(MainChatOverviewUseCase useCase) {
        return useCase;
    }

    @Provides
    public ChatOverviewPresenter provideChatPresenter(MainChatOverviewPresenter presenter) {
        return presenter;
    }

}
