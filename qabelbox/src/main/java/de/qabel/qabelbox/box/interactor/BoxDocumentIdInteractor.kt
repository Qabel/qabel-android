package de.qabel.qabelbox.box.interactor

import android.content.Context
import android.net.Uri
import de.qabel.box.storage.dto.BoxPath
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.client.box.documentId.DocumentId
import de.qabel.client.box.interactor.*
import de.qabel.core.logging.QabelLog
import rx.Observable
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
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

    override fun downloadFile(documentId: DocumentId, targetUri: Uri): Pair<FileOperationState, Observable<FileOperationState>> {
        with(context.contentResolver) {
            debug("open output stream for download")
            openOutputStream(targetUri).let {
                return downloadFile(documentId, it)
            }
        }
    }

    protected fun downloadFile(documentId: DocumentId, targetStream: OutputStream): Pair<FileOperationState, Observable<FileOperationState>> {
        val path = documentId.path
        when (path) {
            is BoxPath.FolderLike -> throw FileNotFoundException("Not a file")
            is BoxPath.File -> {
                return browserByDocumentId(documentId).download(path, targetStream)
            }
        }
    }

    protected fun browserByDocumentId(documentId: DocumentId): OperationFileBrowser
            = volumeManager.operationFileBrowser(documentId.copy(path = BoxPath.Root).toString()).apply {
        debug("Navigated to document id $documentId")
    }

    override fun deletePath(documentId: DocumentId) =
            browserByDocumentId(documentId).delete(documentId.path)

    override fun createFolder(documentId: DocumentId) =
            when (documentId.path) {
                is BoxPath.FolderLike -> browserByDocumentId(documentId).createFolder(documentId.path as BoxPath.FolderLike)
                else -> throw QblStorageException("Not a folder")
            }

}


