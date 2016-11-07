package de.qabel.qabelbox.box.dto

import de.qabel.box.storage.dto.BoxPath
import de.qabel.qabelbox.box.interactor.BoxFileBrowser

data class FileOperationState(val ownerKey: BoxFileBrowser.KeyAndPrefix,
                              val entryName: String, val path: BoxPath.FolderLike,
                              var done: Long, var size: Long,
                              var completed: Boolean = false) {

    val progress: Int
        get() = (100 * done / size).toInt()

}
