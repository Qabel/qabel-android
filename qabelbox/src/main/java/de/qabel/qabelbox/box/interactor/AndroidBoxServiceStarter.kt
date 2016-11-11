package de.qabel.qabelbox.box.interactor

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.qabel.qabelbox.box.AndroidBoxService
import de.qabel.qabelbox.box.provider.DocumentId
import javax.inject.Inject

class AndroidBoxServiceStarter @Inject constructor(private val context: Context) : BoxServiceStarter {

    override fun startUpload(documentId: DocumentId, source: Uri) =
            startBoxService(AndroidBoxService.Actions.UPLOAD_FILE, documentId, source)

    override fun startDownload(documentId: DocumentId, target: Uri) =
            startBoxService(AndroidBoxService.Actions.DOWNLOAD_FILE, documentId, target)

    override fun startCreateFolder(documentId: DocumentId) =
            startBoxService(AndroidBoxService.Actions.CREATE_FOLDER, documentId)

    override fun startDelete(documentId: DocumentId) =
            startBoxService(AndroidBoxService.Actions.DELETE, documentId)

    private fun startBoxService(action: String, documentId: DocumentId, param: Uri? = null) {
        context.startService(Intent(action, param,
                context, AndroidBoxService::class.java).apply {
            putExtra(AndroidBoxService.KEY_DOC_ID, documentId.toString())
        })
    }

}
