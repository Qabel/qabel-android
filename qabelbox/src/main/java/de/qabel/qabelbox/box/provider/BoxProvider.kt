package de.qabel.qabelbox.box.provider

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.provider.MediaStore.Video.Media
import android.util.Log
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.chat.repository.ChatShareRepository
import de.qabel.chat.service.SharingService
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.R
import de.qabel.qabelbox.box.backends.BoxHttpStorageBackend
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.ProviderUpload
import de.qabel.qabelbox.box.dto.UploadSource
import de.qabel.qabelbox.box.interactor.DocumentIdAdapter
import de.qabel.qabelbox.dagger.components.DaggerBoxComponent
import de.qabel.qabelbox.dagger.modules.ContextModule
import de.qabel.qabelbox.storage.server.BlockServer
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import org.jetbrains.anko.info
import org.jetbrains.anko.error
import org.jetbrains.anko.warn
import rx.lang.kotlin.firstOrNull
import java.io.File
import java.io.FileNotFoundException
import java.net.URLConnection
import java.util.*
import javax.inject.Inject

open class BoxProvider : DocumentsProvider(), AnkoLogger {

    @Inject
    lateinit var useCase: DocumentIdAdapter
    @Inject
    lateinit var sharingService: SharingService
    @Inject
    lateinit var sharingRepo: ChatShareRepository
    @Inject
    lateinit var blockServer: BlockServer

    open val handler by lazy { Handler(context.mainLooper) }

    private val volumesChangedBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            notifyRootsUpdated()
        }
    }

    override fun onCreate(): Boolean {
        inject()
        context.registerReceiver(volumesChangedBroadcastReceiver,
                IntentFilter(QblBroadcastConstants.Storage.BOX_VOLUMES_CHANGES))
        return true
    }

    open fun inject() {
        val boxComponent = DaggerBoxComponent.builder()
                .contextModule(ContextModule(context))
                .build()
        boxComponent.inject(this)
    }

    /**
     * Notify the system that the roots have changed
     * This happens if identities or prefixes changed.
     */
    fun notifyRootsUpdated() {
        context.contentResolver.notifyChange(DocumentsContract.buildRootsUri(
                BuildConfig.APPLICATION_ID + AUTHORITY), null)
    }

    @Throws(FileNotFoundException::class)
    override fun queryRoots(projection: Array<String>?): Cursor {
        val netProjection = reduceProjection(projection, DEFAULT_ROOT_PROJECTION)
        val result = MatrixCursor(netProjection)
        useCase.availableRoots().forEach {
            with(result.newRow()) {
                add(Root.COLUMN_ROOT_ID, it.rootID)
                add(Root.COLUMN_DOCUMENT_ID, it.documentID)
                add(Root.COLUMN_ICON, R.drawable.qabel_logo)
                add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE)
                add(Root.COLUMN_TITLE, "Qabel")
                add(Root.COLUMN_SUMMARY, it.alias)
            }
        }
        return result
    }

    private fun reduceProjection(projection: Array<String>?, supportedProjection: Array<String>): Array<String> {
        projection ?: return supportedProjection

        val supported = supportedProjection.toHashSet()
        return projection.filter { it in supported }.let {
            if (it.size == 0) listOf(Document.COLUMN_DOCUMENT_ID)
            else it
        }.toTypedArray()
    }

    @Throws(FileNotFoundException::class)
    override fun queryDocument(documentIdString: String, projection: Array<String>?): Cursor? {
        val id = try {
            documentIdString.toDocumentId()
        } catch (e: QblStorageException) {
            warn("Document $documentIdString not found")
            throw FileNotFoundException("Document not found")
        }
        info("Query document: $documentIdString")
        val entry = useCase.query(id).toBlocking().firstOrNull()
                ?: throw FileNotFoundException("Not found: $documentIdString")
        return createCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION, false).apply {
            when (entry) {
                is BrowserEntry.File -> insertFile(this, id, entry)
                is BrowserEntry.Folder -> insertFolder(this, id, entry)
            }
        }
    }

    @Throws(FileNotFoundException::class)
    override fun queryChildDocuments(parentDocumentId: String, projection: Array<String>?, sortOrder: String?): Cursor {
        val id = try {
            parentDocumentId.toDocumentId()
        } catch (e: QblStorageException) {
            warn("Document $parentDocumentId not found")
            throw FileNotFoundException("Document not found")
        }
        info("Retrieve file listing for $parentDocumentId - $id")
        val listing = useCase.queryChildDocuments(id).toBlocking().firstOrNull() ?: emptyList()
        info("File listing for $parentDocumentId: $listing")
        return createCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION, false).apply {
            listing.map {
                when (it.entry) {
                    is BrowserEntry.File -> insertFile(this, it.documentId, it.entry)
                    is BrowserEntry.Folder -> insertFolder(this, it.documentId, it.entry)
                }
            }
        }
    }

    private fun createCursor(projection: Array<String>, extraLoading: Boolean): BoxCursor {
        val reduced = reduceProjection(projection, DEFAULT_DOCUMENT_PROJECTION)
        return BoxCursor(reduced).apply {
            this.extraLoading = extraLoading
        }
    }

    private fun insertFile(cursor: MatrixCursor, documentId: DocumentId, file: BrowserEntry.File) {
        info("Inserting file into cursor: $documentId - $file")
        val mimeType = URLConnection.guessContentTypeFromName(file.name) ?: "application/octet-stream"
        with(cursor.newRow()) {
            add(Document.COLUMN_DOCUMENT_ID, documentId.toString())
            add(Document.COLUMN_DISPLAY_NAME, file.name)
            add(Document.COLUMN_SUMMARY, null)
            add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_WRITE)
            add(Document.COLUMN_MIME_TYPE, mimeType)
            add(Document.COLUMN_SIZE, file.size)
            add(Media.DATA, documentId.toString())
        }
    }

    private fun insertFolder(cursor: MatrixCursor, documentId: DocumentId, folder: BrowserEntry.Folder) {
        info("Inserting folder into cursor: $documentId - $folder")
        with(cursor.newRow()) {
            add(Document.COLUMN_DOCUMENT_ID, documentId.toString())
            add(Document.COLUMN_DISPLAY_NAME, folder.name)
            add(Document.COLUMN_SUMMARY, null)
            add(Document.COLUMN_FLAGS, Document.FLAG_DIR_SUPPORTS_CREATE)
            add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
        }
    }

    @Throws(FileNotFoundException::class)
    override fun openDocument(documentId: String,
                              mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        try {
            info("Open document $documentId in mode $mode")
            val isWrite = mode.indexOf('w') != -1
            val isRead = mode.indexOf('r') != -1
            val isShare = documentId.startsWith(ShareId.PREFIX)
            val file = File.createTempFile("boxOpen", "tmp", context.externalCacheDir)
            val parsedMode = ParcelFileDescriptor.parseMode(mode)

            if (isShare) {
                val shareId = ShareId.parse(documentId)
                debug("Open share $shareId")
                val share = sharingRepo.findById(shareId.boxShareId)
                sharingService.downloadShare(share, file, BoxHttpStorageBackend(blockServer, ""))
                println(file.exists())
                println(file.length())
                debug("downloaded file ${file.absolutePath}")
            } else {
                val id = documentId.toDocumentId()
                if (isRead) {
                    val download = try {
                        useCase.download(id).toBlocking().firstOrNull()
                    } catch (e: QblStorageException) {
                        throw FileNotFoundException("Download failed")
                    }
                    file.outputStream().run {
                        download.source.source.copyTo(file.outputStream())
                    }
                }
                if (isWrite) {
                    return ParcelFileDescriptor.open(file, parsedMode, handler,
                            ParcelFileDescriptor.OnCloseListener { e ->
                                if (e != null) {
                                    Log.e(TAG, "IOException in onClose", e)
                                    return@OnCloseListener
                                }
                                Log.i(TAG, "Uploading saved file")
                                val entry = BrowserEntry.File(id.path.name, file.length(), Date())
                                file.inputStream().use {
                                    try {
                                        useCase.upload(ProviderUpload(id, UploadSource(it, entry))).toBlocking()
                                    } catch (e: QblStorageException) {
                                        throw FileNotFoundException("Upload failed")
                                    }
                                }
                            })
                }
            }
            return ParcelFileDescriptor.open(file, parsedMode)
        } catch (ex: Throwable) {
            ex.printStackTrace()
            error("Error open document $documentId")
            throw ex
        }
    }


    @Throws(FileNotFoundException::class)
    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
        throw FileNotFoundException("not implemented!")
    }

    @Throws(FileNotFoundException::class)
    override fun deleteDocument(documentId: String) {
        throw FileNotFoundException("not implemented!")
    }

    @Throws(FileNotFoundException::class)
    override fun renameDocument(documentId: String, displayName: String): String {
        throw FileNotFoundException("not implemented!")
    }

    companion object {

        private val TAG = "BoxProvider"

        val DEFAULT_ROOT_PROJECTION = arrayOf(Root.COLUMN_ROOT_ID, Root.COLUMN_MIME_TYPES, Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE, Root.COLUMN_SUMMARY, Root.COLUMN_DOCUMENT_ID)

        val DEFAULT_DOCUMENT_PROJECTION = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS, Document.COLUMN_SIZE, Media.DATA)

        @JvmField
        val AUTHORITY = ".box.provider.documents"
        @JvmField
        val PATH_SEP = "/"
        @JvmField
        val DOCID_SEPARATOR = "::::"
    }
}
