package de.qabel.qabelbox.box.interactor

import android.net.Uri
import de.qabel.client.box.documentId.DocumentId

interface BoxServiceStarter {

    fun startUpload(documentId: DocumentId, source: Uri)
    fun startDownload(documentId: DocumentId, target: Uri)
    fun startCreateFolder(documentId: DocumentId)
    fun startDelete(documentId: DocumentId)

}
