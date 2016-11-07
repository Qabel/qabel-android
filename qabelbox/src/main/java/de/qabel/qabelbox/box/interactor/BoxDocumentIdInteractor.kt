package de.qabel.qabelbox.box.interactor

import android.content.Context
import android.net.Uri
import de.qabel.box.storage.dto.BoxPath
import de.qabel.core.logging.QabelLog
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.FileOperationState
import de.qabel.qabelbox.box.dto.UploadSource
import de.qabel.qabelbox.box.provider.DocumentId
import rx.Observable
import rx.Scheduler
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import javax.inject.Inject

class BoxDocumentIdInteractor @Inject constructor(val context: Context,
                                                  val volumeManager: VolumeManager) : DocumentIdInteractor, QabelLog {

    override fun uploadFile(sourceUri: Uri, targetDocumentId: DocumentId): Pair<FileOperationState, Observable<FileOperationState>> {
        with(context.contentResolver) {
            debug("open input stream for upload")
            openInputStream(sourceUri).let {
                val fileEntry = BrowserEntry.File(targetDocumentId.path.name,
                        it.available().toLong(), Date())
                val path = targetDocumentId.path
                when (path) {
                    is BoxPath.FolderLike -> throw FileNotFoundException("Not a file")
                    is BoxPath.File -> {
                        return browserByDocumentId(targetDocumentId).uploadWithProgress(path, UploadSource(it, fileEntry))
                    }
                }
            }
        }
    }

    override fun downloadFile(documentId: DocumentId, targetFile: File): Pair<FileOperationState, Observable<FileOperationState>> {
        val path = documentId.path
        when (path) {
            is BoxPath.FolderLike -> throw FileNotFoundException("Not a file")
            is BoxPath.File -> {
                return browserByDocumentId(documentId).downloadWithProgress(path, targetFile)
            }
        }
    }

    private fun browserByDocumentId(documentId: DocumentId)
            = volumeManager.fileBrowser(documentId.copy(path = BoxPath.Root).toString()).apply {
        debug("Navigated to document id $documentId")
    }

}


