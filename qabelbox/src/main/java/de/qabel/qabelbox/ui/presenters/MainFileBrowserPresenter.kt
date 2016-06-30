package de.qabel.qabelbox.ui.presenters

import de.qabel.qabelbox.dto.BrowserEntry
import de.qabel.qabelbox.dto.BrowserEntry.*
import de.qabel.qabelbox.ui.views.FileBrowserView
import java.util.*
import javax.inject.Inject

class MainFileBrowserPresenter @Inject constructor(
        private val view: FileBrowserView): FileBrowserPresenter {

    val entries = listOf(File("Name.txt", 42000, Date()))

    override fun onRefresh() = TODO()

    override fun onClick(entry: BrowserEntry) = TODO()
}

