package de.qabel.qabelbox.box.presenters

import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.BrowserEntry.*
import de.qabel.qabelbox.box.views.FileBrowserView
import java.util.*
import javax.inject.Inject

class MainFileBrowserPresenter @Inject constructor(
        private val view: FileBrowserView): FileBrowserPresenter {
    override fun open(file: File) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun share(file: File) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(file: File) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun export(file: File) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteFolder(folder: Folder) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    val entries = listOf(File("Name.txt", 42000, Date()))

    override fun onRefresh() = TODO()

    override fun onClick(entry: BrowserEntry) = TODO()
}

