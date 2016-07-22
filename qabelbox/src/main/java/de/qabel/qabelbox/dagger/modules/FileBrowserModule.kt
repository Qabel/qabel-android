package de.qabel.qabelbox.dagger.modules

import dagger.Module
import dagger.Provides
import de.qabel.qabelbox.box.interactor.BoxFileBrowser
import de.qabel.qabelbox.box.interactor.FileBrowser
import de.qabel.qabelbox.box.interactor.MockFileBrowser
import de.qabel.qabelbox.box.presenters.FileBrowserPresenter
import de.qabel.qabelbox.dagger.scopes.ActivityScope
import de.qabel.qabelbox.box.presenters.MainFileBrowserPresenter
import de.qabel.qabelbox.box.views.FileBrowserView

@ActivityScope
@Module
class FileBrowserModule(private val view: FileBrowserView) {

    @ActivityScope
    @Provides
    fun providerFileBrowserPresenter(useCase: FileBrowser): FileBrowserPresenter {
        return MainFileBrowserPresenter(view, useCase)
    }

    @ActivityScope
    @Provides
    fun provideFileBrowserUseCase(fileBrowserUseCase: MockFileBrowser): FileBrowser {
        return fileBrowserUseCase
    }
}
