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
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.*
import javax.inject.Inject

open class BoxDocumentIdInteractor @Inject constructor(val context: Context,
                                                       val volumeManager: VolumeManager) : DocumentIdInteractor, QabelLog {

    override fun uploadFile(sourceUri: Uri, targetDocumentId: DocumentId): Pair<FileOperationState, Observable<FileOperationState>> {
        with(context.contentResolver) {
            debug("open input stream for upload")
            openInputStream(sourceUri).let {
                return uploadFile(it, targetDocumentId)
            }
        }
    }

    protected fun uploadFile(inputStream: InputStream, targetDocumentId: DocumentId): Pair<FileOperationState, Observable<FileOperationState>> {
        val fileEntry = BrowserEntry.File(targetDocumentId.path.name,
                inputStream.available().toLong(), Date())
        val path = targetDocumentId.path
        when (path) {
            is BoxPath.FolderLike -> throw FileNotFoundException("Not a file")
            is BoxPath.File -> {
                return browserByDocumentId(targetDocumentId).upload(path, UploadSource(inputStream, fileEntry))
            }
        }
    }

    override fun downloadFile(documentId: DocumentId, targetFile: File): Pair<FileOperationState, Observable<FileOperationState>> {
        val path = documentId.path
        when (path) {
            is BoxPath.FolderLike -> throw FileNotFoundException("Not a file")
            is BoxPath.File -> {
                return browserByDocumentId(documentId).download(path, targetFile)
            }
        }
    }

    protected fun browserByDocumentId(documentId: DocumentId): OperationFileBrowser
            = volumeManager.operationFileBrowser(documentId.copy(path = BoxPath.Root).toString()).apply {
        debug("Navigated to document id $documentId")
    }

}


