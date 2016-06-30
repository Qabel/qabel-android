package de.qabel.qabelbox.dagger.modules;

import dagger.Module;
import dagger.Provides;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.ui.presenters.FileBrowserPresenter;
import de.qabel.qabelbox.ui.presenters.MainFileBrowserPresenter;
import de.qabel.qabelbox.ui.views.FileBrowserView;

@ActivityScope
@Module
public class FileBrowserModule {

    private FileBrowserView view;

    public FileBrowserModule(FileBrowserView view) {
        this.view = view;
    }

    @Provides
    public FileBrowserPresenter providerFileBrowserPresenter() {
        return new MainFileBrowserPresenter(view);
    }

}
