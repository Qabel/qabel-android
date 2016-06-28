package de.qabel.qabelbox.providers

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
import de.qabel.desktop.repository.IdentityRepository
import de.qabel.desktop.repository.exception.PersistenceException
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.R
import de.qabel.qabelbox.config.AppPreference
import de.qabel.qabelbox.dagger.components.DaggerBoxComponent
import de.qabel.qabelbox.dagger.modules.ContextModule
import de.qabel.qabelbox.exceptions.QblStorageException
import de.qabel.qabelbox.exceptions.QblStorageNotFound
import de.qabel.qabelbox.storage.BoxManager
import de.qabel.qabelbox.storage.BoxVolume
import de.qabel.qabelbox.storage.model.BoxFolder
import de.qabel.qabelbox.storage.model.BoxObject
import de.qabel.qabelbox.storage.navigation.BoxNavigation
import de.qabel.qabelbox.storage.notifications.StorageNotificationManager
import org.apache.commons.lang3.StringUtils
import java.io.*
import java.net.URLConnection
import java.util.*
import java.util.concurrent.*
import javax.inject.Inject

open class BoxProvider : DocumentsProvider() {

    @Inject
    lateinit var storageNotificationManager: StorageNotificationManager
    @Inject
    lateinit var mDocumentIdParser: DocumentIdParser

    @Inject
    lateinit var identityRepository: IdentityRepository

    @Inject
    lateinit var appPreferences: AppPreference

    @Inject
    lateinit var boxManager: BoxManager

    lateinit private var mThreadPoolExecutor: ThreadPoolExecutor

    lateinit private var folderContentCache: MutableMap<String, BoxCursor>
    lateinit private var currentFolder: String

    private val volumesChangedBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            notifyRootsUpdated()
        }
    }

    override fun onCreate(): Boolean {

        mThreadPoolExecutor = ThreadPoolExecutor(
                2,
                2,
                KEEP_ALIVE_TIME.toLong(),
                KEEP_ALIVE_TIME_UNIT,
                LinkedBlockingDeque<Runnable>())

        folderContentCache = HashMap<String, BoxCursor>()
        val boxComponent = DaggerBoxComponent.builder().contextModule(ContextModule(context)).build()
        boxComponent.inject(this)

        context.registerReceiver(volumesChangedBroadcastReceiver,
                IntentFilter(QblBroadcastConstants.Storage.BOX_VOLUMES_CHANGES))

        return true
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
        try {
            val identities = identityRepository.findAll()
            for (identity in identities.identities) {
                val row = result.newRow()
                val pub_key = identity.ecPublicKey.readableKeyIdentifier
                val prefix: String
                try {
                    prefix = identity.prefixes[0]
                } catch (e: IndexOutOfBoundsException) {
                    Log.e(TAG, "Could not find a prefix in identity " + pub_key)
                    continue
                }

                row.add(Root.COLUMN_ROOT_ID,
                        mDocumentIdParser.buildId(pub_key, prefix, null))
                row.add(Root.COLUMN_DOCUMENT_ID,
                        mDocumentIdParser.buildId(pub_key, prefix, "/"))
                row.add(Root.COLUMN_ICON, R.drawable.qabel_logo)
                row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE)
                row.add(Root.COLUMN_TITLE, "Qabel Box")
                row.add(Root.COLUMN_SUMMARY, identity.alias)
            }
        } catch (e: PersistenceException) {
            throw FileNotFoundException("Error loading identities")
        }

        return result
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
    fun getVolumeForRoot(identity: String, prefix: String): BoxVolume {

        try {
            return boxManager.createBoxVolume(identity, prefix)
        } catch (e: QblStorageException) {
            e.printStackTrace()
            throw FileNotFoundException("Cannot create BoxVolume")
        }

    }

    @Throws(FileNotFoundException::class)
    override fun queryDocument(documentIdString: String, projection: Array<String>?): Cursor? {

        val cursor = createCursor(projection ?: arrayOf(), false)
        try {
            val documentId = mDocumentIdParser.parse(documentIdString)
            var logInfos: String = shrinkDocumentId(documentIdString)
                    ?: throw FileNotFoundException("No documentId")
            if (projection != null) {
                logInfos += " projSize=" + projection.size
            } else {
                logInfos += " projection=null. All fields used"
            }
            Log.v(TAG, "QueryDocument " + logInfos)
            val filePath = documentId.filePath

            val volume = getVolumeForRoot(documentId.identityKey,
                    documentId.prefix)

            if (filePath == PATH_SEP) {
                // root id
                insertRootDoc(cursor, documentIdString)
                return cursor
            }
            val navigation = volume.navigate()
            navigation.navigate(documentId.pathString)
            if (navigation.getFile(documentId.fileName, false) == null) {
                return null
            }
            Log.d(TAG, "Inserting basename " + documentId.fileName)
            insertFileByName(cursor, navigation, documentIdString, documentId.fileName)
        } catch (e: QblStorageException) {
            Log.i(TAG, "Could not find document " + documentIdString, e)
            throw FileNotFoundException("Failed navigating the volume")
        }

        Log.v(TAG, "query roots result, cursorCount=" + cursor.count + " cursorColumn=" + cursor.columnCount)
        return cursor
    }

    private fun shrinkDocumentId(documentId: String?): String? {

        if (documentId == null) {
            return null
        }
        val elements = documentId.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return elements[elements.size - 1]
    }

    @Throws(FileNotFoundException::class)
    private fun getVolumeForId(documentId: String): BoxVolume {

        return getVolumeForRoot(
                mDocumentIdParser.getIdentity(documentId),
                mDocumentIdParser.getPrefix(documentId))
    }

    @Throws(QblStorageException::class)
    internal fun insertFileByName(cursor: MatrixCursor, navigation: BoxNavigation,
                                  documentId: String, basename: String) {

        for (folder in navigation.listFolders()) {
            Log.d(TAG, "Checking folder:" + folder.name)
            if (basename == folder.name) {
                insertFolder(cursor, documentId, folder)
                return
            }
        }
        for (file in navigation.listFiles()) {
            Log.d(TAG, "Checking file:" + file.name)
            if (basename == file.name) {
                insertFile(cursor, documentId, file)
                return
            }
        }
        val external = navigation.getExternal(basename)
        if (external != null) {
            insertFile(cursor, documentId, external)
            return
        }
        throw QblStorageNotFound("File not found")
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

        Log.d(TAG, "Query Child Documents: " + parentDocumentId)
        var cursor: BoxCursor? = folderContentCache[parentDocumentId]
        if (parentDocumentId == currentFolder && cursor != null) {
            // best case: we are still in the same folder and we got a cache hit
            Log.d(TAG, "Up to date cached data found")
            cursor.setExtraLoading(false)
            return cursor
        }
        if (cursor != null) {
            // we found it in the cache, but since we changed the folder, we refresh anyway
            cursor.setExtraLoading(true)
        } else {
            Log.d(TAG, "Serving empty listing and refreshing")
            cursor = createCursor(projection, true)
        }
        currentFolder = parentDocumentId
        asyncChildDocuments(parentDocumentId, projection, cursor)
        return cursor
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

        Log.v(TAG, "createBoxCursor")
        val cursor = createCursor(projection, false)
        try {
            val parentId = mDocumentIdParser.parse(parentDocumentId)
            val volume = getVolumeForRoot(parentId.identityKey, parentId.prefix)

            val navigation = volume.navigate()
            navigation.navigate(parentId.pathString)
            insertFolderListing(cursor, navigation, parentDocumentId)
        } catch (e: QblStorageException) {
            Log.e(TAG, "Could not navigate", e)
            throw FileNotFoundException("Failed navigating the volume")
        }

        folderContentCache.put(parentDocumentId, cursor)
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

        // create a new cursor and store it
        mThreadPoolExecutor.execute {
            try {
                createBoxCursor(parentDocumentId, projection)
            } catch (e: FileNotFoundException) {
                val cursor = createCursor(projection, false)
                cursor.setError(context.getString(R.string.folderListingUpdateError))
                folderContentCache.put(parentDocumentId, cursor)
            }

            context.contentResolver.notifyChange(uri, null)
        }
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
        for (file in navigation.listExternalNames()) {
            insertFile(cursor, parentDocumentId + file.name, file)
        }
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

        if (isWrite) {
            // Attach a close listener if the document is opened in write mode.
            try {
                val handler = Handler(context.mainLooper)
                val tmp: File
                if (isRead) {
                    tmp = downloadFile(documentId)
                } else {
                    tmp = File.createTempFile("uploadAndDeleteLocalfile", "", context.externalCacheDir)
                }
                val onCloseListener = ParcelFileDescriptor.OnCloseListener { e ->
                    // Update the file with the cloud server.  The client is done writing.
                    Log.i(TAG, "A file with id $documentId has been closed!  Time to update the server.")
                    if (e != null) {
                        Log.e(TAG, "IOException in onClose", e)
                        return@OnCloseListener
                    }
                    // in another thread!
                    object : AsyncTask<Void, Void, String>() {
                        override fun doInBackground(vararg params: Void): String {
                            try {
                                val documentId1 = mDocumentIdParser.parse(documentId)
                                val path = documentId1.pathString
                                val volume = getVolumeForRoot(documentId1.identityKey,
                                        documentId1.prefix)
                                val boxNavigation = volume.navigate()
                                boxNavigation.navigate(path)
                                boxNavigation.upload(documentId1.fileName,
                                        FileInputStream(tmp))
                                boxNavigation.commit()
                            } catch (e1: FileNotFoundException) {
                                Log.e(TAG, "Cannot upload file!", e1)
                            } catch (e1: QblStorageException) {
                                Log.e(TAG, "Cannot upload file!", e1)
                            }

                            Log.d(TAG, "UPLOAD DONE")
                            return documentId
                        }
                    }.execute()
                }
                return ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.parseMode(mode), handler,
                        onCloseListener)
            } catch (e: IOException) {
                throw FileNotFoundException()
            }

        } else {
            val tmp = downloadFile(documentId)
            val accessMode = ParcelFileDescriptor.parseMode(mode)
            return ParcelFileDescriptor.open(tmp, accessMode)
        }
    }

    @Throws(FileNotFoundException::class)
    private fun downloadFile(documentId: String): File {

        val future = mThreadPoolExecutor.submit(
                Callable { this@BoxProvider.getFile(documentId) })
        try {
            return future.get()
        } catch (e: InterruptedException) {
            Log.d(TAG, "openDocument cancelled download")
            throw FileNotFoundException()
        } catch (e: ExecutionException) {
            Log.d(TAG, "Execution error", e)
            throw FileNotFoundException()
        }

    }

    @Throws(IOException::class, QblStorageException::class)
    private fun getFile(documentId: String): File {
        return boxManager.downloadFileDecrypted(documentId)
    }

    @Throws(FileNotFoundException::class)
    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {

        Log.d(TAG, "createDocument: $parentDocumentId; $mimeType; $displayName")

        try {

            val parentId = mDocumentIdParser.parse(parentDocumentId)
            val parentPath = parentId.filePath
            val volume = getVolumeForRoot(parentId.identityKey, parentId.prefix)


            val navigation = volume.navigate()
            navigation.navigate(parentPath)

            if (mimeType == Document.MIME_TYPE_DIR) {
                navigation.createFolder(displayName)
            } else {
                navigation.upload(displayName, ByteArrayInputStream(ByteArray(0)))
            }
            navigation.commit()

            return parentDocumentId + displayName
        } catch (e: QblStorageException) {
            Log.e(TAG, "could not create file", e)
            throw FileNotFoundException()
        }

    }

    @Throws(FileNotFoundException::class)
    override fun deleteDocument(documentId: String) {

        Log.d(TAG, "deleteDocument: " + documentId)

        try {

            val document = mDocumentIdParser.parse(documentId)
            val volume = getVolumeForRoot(document.identityKey, document.prefix)
            val navigation = volume.navigate()
            navigation.navigate(document.pathString)

            val basename = document.fileName
            for (file in navigation.listFiles()) {
                if (file.name == basename) {
                    navigation.delete(file)
                    navigation.commit()
                    return
                }
            }
            for (folder in navigation.listFolders()) {
                if (folder.name == basename) {
                    navigation.delete(folder)
                    navigation.commit()
                    return
                }
            }
        } catch (e: QblStorageException) {
            Log.e(TAG, "could not create file", e)
            throw FileNotFoundException()
        }

    }

    @Throws(FileNotFoundException::class)
    override fun renameDocument(documentId: String, displayName: String): String {

        Log.d(TAG, "renameDocument: $documentId to $displayName")

        try {

            val document = mDocumentIdParser.parse(documentId)
            val volume = getVolumeForId(documentId)

            val splitPath = document.path
            val basename = document.fileName
            val navigation = volume.navigate()

            val newPath = Arrays.copyOf(splitPath, splitPath.size + 1)
            newPath[newPath.size - 1] = displayName

            val renamedId = mDocumentIdParser.buildId(
                    mDocumentIdParser.getIdentity(documentId),
                    mDocumentIdParser.getPrefix(documentId),
                    StringUtils.join(newPath, PATH_SEP))

            for (file in navigation.listFiles()) {
                if (file.name == basename) {
                    navigation.rename(file, displayName)
                    navigation.commit()
                    return renamedId
                }
            }
            for (folder in navigation.listFolders()) {
                if (folder.name == basename) {
                    navigation.rename(folder, displayName)
                    navigation.commit()
                    return renamedId
                }
            }
            throw FileNotFoundException()
        } catch (e: QblStorageException) {
            Log.e(TAG, "could not create file", e)
            throw FileNotFoundException()
        }

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

        private val KEEP_ALIVE_TIME = 1
        private val KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS
    }
}
