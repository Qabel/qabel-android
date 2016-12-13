package de.qabel.qabelbox.dagger.modules

import dagger.Module
import dagger.Provides
import de.qabel.qabelbox.box.presenters.ExternalFileUploadPresenter
import de.qabel.qabelbox.box.presenters.FileUploadPresenter
import de.qabel.qabelbox.box.views.FileUploadView
import de.qabel.qabelbox.dagger.scopes.ActivityScope

@ActivityScope
@Module
class ExternalFileUploadModule(private val view: FileUploadView) {

    @ActivityScope
    @Provides
    fun provideFileUploadView() = view

    @ActivityScope
    @Provides
    fun provideFileUploadPresenter(fileUploadPresenter: ExternalFileUploadPresenter): FileUploadPresenter
            = fileUploadPresenter
}

