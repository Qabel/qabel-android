package de.qabel.qabelbox.box.dagger.modules;

import dagger.Module;
import dagger.Provides;
import de.qabel.qabelbox.box.interactor.FileBrowserUseCase;
import de.qabel.qabelbox.box.interactor.MockFileBrowserUseCase;
import de.qabel.qabelbox.box.presenters.FileBrowserPresenter;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.box.presenters.MainFileBrowserPresenter;
import de.qabel.qabelbox.box.views.FileBrowserView;

@ActivityScope
@Module
public class FileBrowserModule {

    private FileBrowserView view;

    public FileBrowserModule(FileBrowserView view) {
        this.view = view;
    }

    @Provides
    public FileBrowserPresenter providerFileBrowserPresenter(FileBrowserUseCase useCase) {
        return new MainFileBrowserPresenter(view, useCase);
    }

    @Provides
    public FileBrowserUseCase provideFileBrowserUseCase(MockFileBrowserUseCase mockFileBrowserUseCase) {
        return mockFileBrowserUseCase;
    }
}
