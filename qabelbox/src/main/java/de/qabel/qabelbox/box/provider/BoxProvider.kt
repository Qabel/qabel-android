package de.qabel.qabelbox.box.provider

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.provider.MediaStore.Video.Media
import android.util.Log
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.R
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.interactor.ProviderUseCase
import de.qabel.qabelbox.dagger.components.DaggerBoxComponent
import de.qabel.qabelbox.dagger.modules.ContextModule
import rx.lang.kotlin.firstOrNull
import java.io.File
import java.io.FileNotFoundException
import java.net.URLConnection

open class BoxProvider : DocumentsProvider() {

    lateinit var useCase: ProviderUseCase

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
        val boxComponent = DaggerBoxComponent.builder().contextModule(ContextModule(context)).build()
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
    override fun queryRoots(projection: Array<String>): Cursor {
        val netProjection = reduceProjection(projection, DEFAULT_ROOT_PROJECTION)
        val result = MatrixCursor(netProjection)
        useCase.availableRoots().forEach {
            with(it) {
                with(result.newRow()) {
                    add(Root.COLUMN_ROOT_ID, rootID)
                    add(Root.COLUMN_DOCUMENT_ID, documentID)
                    add(Root.COLUMN_ICON, R.drawable.qabel_logo)
                    add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE)
                    add(Root.COLUMN_TITLE, "Qabel")
                    add(Root.COLUMN_SUMMARY, alias)
                }
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
        val id = try { documentIdString.toDocumentId() } catch (e: QblStorageException) {
            throw FileNotFoundException("Document not found") }
        val entry = useCase.query(id).toBlocking().firstOrNull()
                ?: throw FileNotFoundException("Not found: $documentIdString")
        return createCursor(projection ?: arrayOf(), false).apply {
            when (entry) {
                is BrowserEntry.File -> insertFile(this, id, entry)
                is BrowserEntry.Folder -> insertFolder(this, id, entry)
            }
        }
    }

    internal fun insertRootDoc(cursor: MatrixCursor, documentId: String) {

        val row = cursor.newRow()
        row.add(Document.COLUMN_DOCUMENT_ID, documentId)
        row.add(Document.COLUMN_DISPLAY_NAME, "Root")
        row.add(Document.COLUMN_SUMMARY, null)
        row.add(Document.COLUMN_FLAGS, Document.FLAG_DIR_SUPPORTS_CREATE)
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
    }

    @Throws(FileNotFoundException::class)
    override fun queryChildDocuments(parentDocumentId: String, projection: Array<String>, sortOrder: String): Cursor {
        TODO()
    }

    /**
     * Create and fill a new MatrixCursor
     *
     *
     * The cursor can be modified to show a loading and/or an error message.

     * @param parentDocumentId
     * *
     * @param projection
     * *
     * @return Fully initialized cursor with the directory listing as rows
     * *
     * @throws FileNotFoundException
     */
    @Throws(FileNotFoundException::class)
    private fun createBoxCursor(parentDocumentId: String, projection: Array<String>): BoxCursor {
        val cursor = createCursor(projection, false)
        TODO()
        return cursor
    }

    /**
     * Query the directory listing, store the cursor in the folderContentCache and
     * notify the original cursor of the update.

     * @param parentDocumentId
     * *
     * @param projection
     * *
     * @param result           Original cursor
     */
    private fun asyncChildDocuments(parentDocumentId: String, projection: Array<String>,
                                    result: BoxCursor) {

        Log.v(TAG, "asyncChildDocuments")
        val uri = DocumentsContract.buildChildDocumentsUri(
                BuildConfig.APPLICATION_ID + AUTHORITY, parentDocumentId)
        // tell the original cursor how he gets notified
        result.setNotificationUri(context.contentResolver, uri)
        TODO()
    }

    private fun createCursor(projection: Array<String>, extraLoading: Boolean): BoxCursor {

        val reduced = reduceProjection(projection, DEFAULT_DOCUMENT_PROJECTION)
        return BoxCursor(reduced).apply {
            this.extraLoading = extraLoading
        }
    }

    private fun insertFile(cursor: MatrixCursor, documentId: DocumentId, file: BrowserEntry.File) {

        val mimeType = URLConnection.guessContentTypeFromName(file.name) ?: "application/octet-stream"
        with(cursor.newRow()) {
            add(Document.COLUMN_DOCUMENT_ID, documentId.toString())
            add(Document.COLUMN_DISPLAY_NAME, file.name)
            add(Document.COLUMN_SUMMARY, null)
            add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_WRITE)
            add(Document.COLUMN_MIME_TYPE, mimeType)
            add(Media.DATA, documentId.toString())
        }
    }

    private fun insertFolder(cursor: MatrixCursor, documentId: DocumentId, folder: BrowserEntry.Folder) {
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
        val isWrite = mode.indexOf('w') != -1
        val isRead = mode.indexOf('r') != -1
        if (isWrite) {
            TODO()
        }
        val file = File.createTempFile("boxOpen", "tmp", context.externalCacheDir)
        if (isRead) {
            val download = useCase.download(documentId.toDocumentId()).toBlocking().firstOrNull()
            file.outputStream().run {
                download.source.source.copyTo(file.outputStream())
            }
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
    }


    @Throws(FileNotFoundException::class)
    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
        TODO()
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
        val AUTHORITY = ".providers.documents"
        @JvmField
        val PATH_SEP = "/"
        @JvmField
        val DOCID_SEPARATOR = "::::"
    }
}
