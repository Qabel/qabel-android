package de.qabel.qabelbox.box.views

import de.qabel.box.storage.dto.BoxPath
import de.qabel.qabelbox.contacts.dto.EntitySelection
import de.qabel.qabelbox.box.presenters.FileUploadPresenter
import de.qabel.qabelbox.box.provider.DocumentId

interface FileUploadView {

    var identity: EntitySelection
    var path: BoxPath.FolderLike
    var filename: String

    fun startUpload(documentId: DocumentId)

}

