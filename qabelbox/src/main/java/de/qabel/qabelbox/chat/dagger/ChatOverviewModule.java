package de.qabel.qabelbox.chat.dagger;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.repository.ContactRepository;
import de.qabel.core.repository.IdentityRepository;
import de.qabel.qabelbox.chat.interactor.FindLatestConversations;
import de.qabel.qabelbox.chat.interactor.MainFindLatestConversations;
import de.qabel.qabelbox.chat.transformers.ChatMessageTransformer;
import de.qabel.qabelbox.chat.view.presenters.ChatOverviewPresenter;
import de.qabel.qabelbox.chat.view.presenters.MainChatOverviewPresenter;
import de.qabel.qabelbox.chat.view.views.ChatOverview;
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
    public FindLatestConversations provideChatUseCase(MainFindLatestConversations useCase) {
        return useCase;
    }

    @Provides
    public ChatOverviewPresenter provideChatPresenter(MainChatOverviewPresenter presenter) {
        return presenter;
    }

}
