package de.qabel.qabelbox.dagger.modules

import dagger.Module
import dagger.Provides
import de.qabel.qabelbox.box.presenters.FolderChooserPresenter
import de.qabel.qabelbox.box.presenters.MainFolderChooserPresenter
import de.qabel.qabelbox.box.views.FileListingView
import de.qabel.qabelbox.box.views.FolderChooserView
import de.qabel.qabelbox.dagger.scopes.ActivityScope

@ActivityScope
@Module
class FolderChooserModule(private val view: FolderChooserView) {

    @ActivityScope
    @Provides
    fun provideFolderChooserView(): FolderChooserView = view

    @ActivityScope
    @Provides
    fun provideFileListingView(): FileListingView {
        return view
    }

    @ActivityScope
    @Provides
    fun provideFolderChooserPresenter(folderChooserPresenter: MainFolderChooserPresenter):
            FolderChooserPresenter = folderChooserPresenter

}
