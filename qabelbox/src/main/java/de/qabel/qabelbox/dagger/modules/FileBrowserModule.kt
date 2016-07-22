package de.qabel.qabelbox.dagger.modules

import dagger.Module
import dagger.Provides
import de.qabel.qabelbox.box.interactor.BoxFileBrowserUseCase
import de.qabel.qabelbox.box.interactor.FileBrowserUseCase
import de.qabel.qabelbox.box.interactor.MockFileBrowserUseCase
import de.qabel.qabelbox.box.presenters.FileBrowserPresenter
import de.qabel.qabelbox.dagger.scopes.ActivityScope
import de.qabel.qabelbox.box.presenters.MainFileBrowserPresenter
import de.qabel.qabelbox.box.views.FileBrowserView

@ActivityScope
@Module
class FileBrowserModule(private val view: FileBrowserView) {

    @ActivityScope
    @Provides
    fun providerFileBrowserPresenter(useCase: FileBrowserUseCase): FileBrowserPresenter {
        return MainFileBrowserPresenter(view, useCase)
    }

    @ActivityScope
    @Provides
    fun provideFileBrowserUseCase(fileBrowserUseCase: MockFileBrowserUseCase): FileBrowserUseCase {
        return fileBrowserUseCase
    }
}
