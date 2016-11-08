package de.qabel.qabelbox.box.presenters

import de.qabel.box.storage.dto.BoxPath

interface NavigatingPresenter {
    var path : BoxPath.FolderLike

    fun onRefresh()
    fun navigateUp(): Boolean


}

