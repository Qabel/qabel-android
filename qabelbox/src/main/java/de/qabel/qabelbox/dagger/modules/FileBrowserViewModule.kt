package de.qabel.qabelbox.dagger.modules

import dagger.Module
import dagger.Provides
import de.qabel.qabelbox.box.views.FileBrowserView
import de.qabel.qabelbox.box.views.FileListingView
import de.qabel.qabelbox.dagger.scopes.ActivityScope

@ActivityScope
@Module
class FileBrowserViewModule(private val view: FileBrowserView) {

    @ActivityScope
    @Provides
    fun provideFileBrowserView(): FileBrowserView {
        return view
    }

    @ActivityScope
    @Provides
    fun provideFileListingView(): FileListingView {
        return view
    }

}

