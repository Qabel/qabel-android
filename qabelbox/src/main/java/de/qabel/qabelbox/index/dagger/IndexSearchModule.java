package de.qabel.qabelbox.index.dagger;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.repository.ContactRepository;
import de.qabel.core.repository.IdentityRepository;
import de.qabel.qabelbox.chat.interactor.ChatOverviewUseCase;
import de.qabel.qabelbox.chat.interactor.MainChatOverviewUseCase;
import de.qabel.qabelbox.chat.transformers.ChatMessageTransformer;
import de.qabel.qabelbox.chat.view.presenters.ChatOverviewPresenter;
import de.qabel.qabelbox.chat.view.presenters.MainChatOverviewPresenter;
import de.qabel.qabelbox.chat.view.views.ChatOverview;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.index.interactor.IndexSearchUseCase;
import de.qabel.qabelbox.index.interactor.MainIndexSearchUseCase;
import de.qabel.qabelbox.index.view.presenters.IndexSearchPresenter;
import de.qabel.qabelbox.index.view.presenters.MainIndexSearchPresenter;
import de.qabel.qabelbox.index.view.views.IndexSearchView;

@ActivityScope
@Module
public class IndexSearchModule {

    private IndexSearchView view;

    public IndexSearchModule(IndexSearchView view) {
        this.view = view;
    }

    @Provides
    public IndexSearchView provideIndexSearchView() {
        return view;
    }

    @Provides
    public IndexSearchUseCase provideIndexSearchUseCase(MainIndexSearchUseCase useCase) {
        return useCase;
    }

    @Provides
    public IndexSearchPresenter provideIndexSearchPresenter(MainIndexSearchPresenter presenter) {
        return presenter;
    }

}
