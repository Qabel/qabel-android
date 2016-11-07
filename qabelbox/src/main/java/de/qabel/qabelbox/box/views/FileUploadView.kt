package de.qabel.qabelbox.box.views

import de.qabel.box.storage.dto.BoxPath
import de.qabel.qabelbox.box.presenters.FileUploadPresenter
import de.qabel.qabelbox.box.provider.DocumentId

interface FileUploadView {

    val identity: FileUploadPresenter.IdentitySelection
    val path: BoxPath
    val filename: String

    fun startUpload(documentId: DocumentId)

}

