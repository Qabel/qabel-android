package de.qabel.qabelbox.box.provider

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.database.MatrixCursor
import android.os.AsyncTask
import android.os.CancellationSignal
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.provider.MediaStore.Video.Media
import android.util.Log
import de.qabel.box.storage.BoxFolder
import de.qabel.box.storage.BoxNavigation
import de.qabel.box.storage.BoxObject
import de.qabel.box.storage.BoxVolume
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.box.storage.exceptions.QblStorageNotFound
import de.qabel.desktop.repository.IdentityRepository
import de.qabel.desktop.repository.exception.PersistenceException
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.R
import de.qabel.qabelbox.config.AppPreference
import de.qabel.qabelbox.dagger.components.DaggerBoxComponent
import de.qabel.qabelbox.dagger.modules.ContextModule
import de.qabel.qabelbox.storage.notifications.StorageNotificationManager
import java.io.*
import java.net.URLConnection
import java.util.*
import java.util.concurrent.*
import javax.inject.Inject

open class BoxProvider : DocumentsProvider() {

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

    internal fun inject() {
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
        TODO()
    }

    private fun reduceProjection(projection: Array<String>?, supportedProjection: Array<String>): Array<String> {

        if (projection == null) {
            return supportedProjection
        }
        val supported = HashSet(Arrays.asList(*supportedProjection))
        val result = ArrayList<String>()
        for (column in projection) {
            if (supported.contains(column)) {
                result.add(column)
            } else {
                Log.w(TAG, "Requested cursor field don't supported '$column'")
            }
        }
        if (result.size == 0) {
            Log.e(TAG, "Cursors contain no fields after reduceProjection. Add fallback field")
            //add fallback if no field supported. this avoid crashes on different third party apps
            result.add(Document.COLUMN_DOCUMENT_ID)
        }

        return result.toTypedArray()
    }

    @Throws(FileNotFoundException::class)
    override fun queryDocument(documentIdString: String, projection: Array<String>?): Cursor? {

        val cursor = createCursor(projection ?: arrayOf(), false)
        return cursor
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
        val cursor = BoxCursor(reduced)
        cursor.setExtraLoading(extraLoading)
        return cursor
    }

    @Throws(QblStorageException::class)
    private fun insertFolderListing(cursor: MatrixCursor, navigation: BoxNavigation, parentDocumentId: String) {

        for (folder in navigation.listFolders()) {
            insertFolder(cursor, parentDocumentId + folder.name + PATH_SEP, folder)
        }
        for (file in navigation.listFiles()) {
            insertFile(cursor, parentDocumentId + file.name, file)
        }
        /*
        for (file in navigation.listExternalNames()) {
            insertFile(cursor, parentDocumentId + file.name, file)
        }
        */
    }

    private fun insertFile(cursor: MatrixCursor, documentId: String, file: BoxObject) {

        val row = cursor.newRow()
        var mimeType: String? = URLConnection.guessContentTypeFromName(file.name)
        if (mimeType == null) {
            mimeType = "application/octet-stream"
        }
        row.add(Document.COLUMN_DOCUMENT_ID, documentId)
        row.add(Document.COLUMN_DISPLAY_NAME, file.name)
        row.add(Document.COLUMN_SUMMARY, null)
        row.add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_WRITE)
        row.add(Document.COLUMN_MIME_TYPE, mimeType)
        row.add(Media.DATA, documentId)
    }

    private fun insertFolder(cursor: MatrixCursor, documentId: String, folder: BoxFolder) {

        val row = cursor.newRow()
        row.add(Document.COLUMN_DOCUMENT_ID, documentId)
        row.add(Document.COLUMN_DISPLAY_NAME, folder.name)
        row.add(Document.COLUMN_SUMMARY, null)
        row.add(Document.COLUMN_FLAGS, Document.FLAG_DIR_SUPPORTS_CREATE)
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
    }

    @Throws(FileNotFoundException::class)
    override fun openDocument(documentId: String,
                              mode: String, signal: CancellationSignal): ParcelFileDescriptor {

        Log.d(TAG, "Open document: " + documentId)
        val isWrite = mode.indexOf('w') != -1
        val isRead = mode.indexOf('r') != -1
        TODO()
    }


    @Throws(FileNotFoundException::class)
    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
        TODO()
    }

    @Throws(FileNotFoundException::class)
    override fun deleteDocument(documentId: String) {
        TODO()
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
