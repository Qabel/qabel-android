package de.qabel.qabelbox.box.interactor

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.qabel.qabelbox.box.AndroidBoxService
import de.qabel.qabelbox.box.provider.DocumentId
import javax.inject.Inject

class AndroidBoxServiceStarter @Inject constructor(private val context: Context) : BoxServiceStarter {

    override fun startUpload(documentId: DocumentId, source: Uri) {
        context.startService(Intent(AndroidBoxService.Actions.UPLOAD_FILE, source,
                context, AndroidBoxService::class.java).apply {
            putExtra(AndroidBoxService.KEY_DOC_ID, documentId.toString())
        })
    }

    override fun startDownload(documentId: DocumentId, target: Uri) {
        context.startService(Intent(AndroidBoxService.Actions.DOWNLOAD_FILE, target,
                context, AndroidBoxService::class.java).apply {
            putExtra(AndroidBoxService.KEY_DOC_ID, documentId.toString())
        })
    }
}
