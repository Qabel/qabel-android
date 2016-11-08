package de.qabel.qabelbox.box.interactor

import android.net.Uri
import de.qabel.qabelbox.box.provider.DocumentId

interface BoxServiceStarter {

    fun startUpload(documentId: DocumentId, source: Uri)
    fun startDownload(documentId: DocumentId, target: Uri)

}
