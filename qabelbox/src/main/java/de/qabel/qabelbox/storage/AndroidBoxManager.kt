package de.qabel.qabelbox.storage

import android.content.Context
import android.content.Intent

import org.apache.commons.io.IOUtils
import org.spongycastle.crypto.params.KeyParameter

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.InvalidKeyException
import java.util.Collections
import java.util.HashMap
import java.util.LinkedList
import java.util.Queue
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue

import javax.inject.Inject

import de.qabel.core.config.Identity
import de.qabel.core.crypto.CryptoUtils
import de.qabel.core.crypto.QblECKeyPair
import de.qabel.desktop.repository.IdentityRepository
import de.qabel.desktop.repository.exception.EntityNotFoundException
import de.qabel.desktop.repository.exception.PersistenceException
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.box.AndroidBoxVolume
import de.qabel.qabelbox.config.AppPreference
import de.qabel.qabelbox.exceptions.QblServerException
import de.qabel.qabelbox.exceptions.QblStorageException
import de.qabel.qabelbox.exceptions.QblStorageNotFound
import de.qabel.qabelbox.providers.DocumentId
import de.qabel.qabelbox.providers.DocumentIdParser
import de.qabel.qabelbox.services.StorageBroadcastConstants
import de.qabel.qabelbox.storage.model.BoxFile
import de.qabel.qabelbox.storage.model.BoxUploadingFile
import de.qabel.qabelbox.storage.navigation.BoxNavigation
import de.qabel.qabelbox.storage.notifications.StorageNotificationManager
import de.qabel.qabelbox.storage.transfer.BoxTransferListener
import de.qabel.qabelbox.storage.transfer.TransferManager

class AndroidBoxManager
@Inject
constructor(internal var context:

            Context,
            internal var storageNotificationManager: StorageNotificationManager,
            internal var documentIdParser: DocumentIdParser,
            internal var appPreferences: AppPreference,
            internal var transferManager: TransferManager,
            internal var identityRepository: IdentityRepository) : BoxManager {

    private val fileCache: FileCache
    private val cryptoUtils: CryptoUtils

    private inner class UploadResult(var mTime: Long, var size: Long)

    init {
        this.fileCache = FileCache(context)
        this.cryptoUtils = CryptoUtils()
    }

    override fun getCryptoUtils(): CryptoUtils {
        return cryptoUtils
    }

    override fun getCachedFinishedUploads(path: String): Collection<BoxFile>? {
        val files = cachedFinishedUploads[path]
        if (files != null) {
            return files.values
        }
        return null
    }

    override fun clearCachedUploads(path: String) {
        cachedFinishedUploads.remove(path)
    }

    override fun getPendingUploads(path: String): List<BoxUploadingFile> {
        val uploadingFiles = LinkedList<BoxUploadingFile>()
        for (f in uploadingQueue) {
            if (f.path == path) {
                uploadingFiles.add(f)
            }
        }
        return uploadingFiles
    }

    @Throws(QblStorageException::class)
    protected fun addUploadTransfer(documentId: DocumentId): BoxTransferListener {

        val boxUploadingFile = BoxUploadingFile(documentId.fileName,
                documentId.pathString, documentId.identityKey)

        uploadingQueue.add(boxUploadingFile)
        updateUploadNotifications()
        broadcastUploadStatus(documentId.toString(), StorageBroadcastConstants.UPLOAD_STATUS_NEW)

        return object : BoxTransferListener {
            override fun onProgressChanged(bytesCurrent: Long, bytesTotal: Long) {
                boxUploadingFile.totalSize = bytesTotal
                boxUploadingFile.uploadedSize = bytesCurrent
                updateUploadNotifications()
            }

            override fun onFinished() {
                boxUploadingFile.uploadedSize = boxUploadingFile.totalSize
                updateUploadNotifications()
            }
        }
    }

    private fun broadcastUploadStatus(documentId: String, uploadStatus: Int) {
        val intent = Intent(QblBroadcastConstants.Storage.BOX_UPLOAD_CHANGED)
        intent.putExtra(StorageBroadcastConstants.EXTRA_UPLOAD_DOCUMENT_ID, documentId)
        intent.putExtra(StorageBroadcastConstants.EXTRA_UPLOAD_STATUS, uploadStatus)
        context.sendBroadcast(intent)
    }


    private fun updateUploadNotifications() {
        storageNotificationManager.updateUploadNotification(uploadingQueue.size, uploadingQueue.peek())
    }

    @Throws(QblStorageException::class)
    private fun removeUpload(documentId: String, cause: Int, resultFile: BoxFile?) {
        uploadingQueue.poll()
        if (resultFile != null) {
            when (cause) {
                StorageBroadcastConstants.UPLOAD_STATUS_FINISHED ->
                    cacheFinishedUpload(documentId, resultFile)
            }
        }
        updateUploadNotifications()
        broadcastUploadStatus(documentId, cause)
    }

    private fun cacheFinishedUpload(documentId: String, boxFile: BoxFile) {
        try {
            var cachedFiles = cachedFinishedUploads[documentIdParser.getPath(documentId)] ?: mutableMapOf()
            cachedFiles[boxFile.name] = boxFile
            cachedFinishedUploads.put(documentIdParser.getPath(documentId), cachedFiles)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

    }

    @Throws(QblStorageException::class)
    override fun createBoxVolume(identity: String, prefix: String): BoxVolume {
        try {
            val retrievedIdentity = identityRepository.find(identity) ?: throw RuntimeException("Identity " + identity + "is unknown!")
            val key = retrievedIdentity.primaryKeyPair

            val deviceId = appPreferences.deviceId
            return BoxVolume(key, prefix, deviceId, context, this)
        } catch (e: EntityNotFoundException) {
            throw QblStorageException("Cannot create BoxVolume")
        } catch (e: PersistenceException) {
            throw QblStorageException("Cannot create BoxVolume")
        }

    }

    @Throws(QblStorageException::class)
    override fun createBoxVolume(identity: Identity): BoxVolume {
        return createBoxVolume(identity.keyIdentifier, identity.prefixes[0])
    }

    override fun notifyBoxVolumesChanged() {
        context.sendBroadcast(Intent(QblBroadcastConstants.Storage.BOX_VOLUMES_CHANGES))
    }

    private fun notifyBoxChanged() {
        context.sendBroadcast(Intent(QblBroadcastConstants.Storage.BOX_CHANGED))
    }

    @Throws(QblStorageException::class)
    override fun downloadFileDecrypted(documentIdString: String): File {
        val documentId = documentIdParser.parse(documentIdString)

        val volume = createBoxVolume(documentId.identityKey, documentId.prefix)
        val navigation = volume.navigate()
        navigation.navigate(documentId.filePath)

        val file = navigation.getFile(documentId.fileName, true)
        return downloadFileDecrypted(file, documentId.identityKey, documentId.pathString)
    }

    @Throws(QblStorageException::class)
    override fun downloadStreamDecrypted(boxFile: BoxFile, identityKeyIdentifier: String, path: String): InputStream {
        try {
            val file = downloadFileDecrypted(boxFile, identityKeyIdentifier, path)
            return FileInputStream(file)
        } catch (e: IOException) {
            throw QblStorageException(e)
        }

    }

    @Throws(QblStorageException::class)
    override fun downloadFileDecrypted(boxFile: BoxFile, identityKeyIdentifier: String, path: String): File {
        val file = fileCache.get(boxFile)
        if (file != null) {
            return file
        }

        val downloadedFile = blockingDownload(boxFile.prefix,
                BoxManager.BLOCKS_PREFIX + boxFile.block,
                storageNotificationManager.addDownloadNotification(identityKeyIdentifier, path, boxFile))

        val outputFile = File(context.externalCacheDir, boxFile.name)
        decryptFile(boxFile.key, downloadedFile, outputFile)
        fileCache.put(boxFile, outputFile)

        return outputFile
    }

    @Throws(QblStorageException::class)
    override fun blockingDownload(prefix: String, name: String, boxTransferListener: BoxTransferListener?): File {
        val target = transferManager.createTempFile()
        val id = transferManager.download(prefix, name, target, boxTransferListener)
        if (transferManager.waitFor(id)) {
            return target
        } else {
            try {
                throw transferManager.lookupError(id)
            } catch (e: QblServerException) {
                if (e.statusCode == 404) {
                    throw QblStorageNotFound("File not found. Prefix: $prefix Name: $name")
                }
                throw QblStorageException(e)
            } catch (e: Exception) {
                throw QblStorageException(e)
            }

        }
    }

    @Throws(QblStorageException::class)
    private fun decryptFile(boxFileKey: ByteArray, sourceFile: File, targetFile: File) {
        val key = KeyParameter(boxFileKey)
        try {
            if (!cryptoUtils.decryptFileAuthenticatedSymmetricAndValidateTag(
                    FileInputStream(sourceFile), targetFile, key) || targetFile.length() == 0L) {
                throw QblStorageException("Decryption failed")
            }
        } catch (e: IOException) {
            throw QblStorageException(e)
        } catch (e: InvalidKeyException) {
            throw QblStorageException(e)
        }

    }

    @Throws(QblStorageException::class)
    override fun downloadDecrypted(prefix: String, name: String, key: ByteArray, boxTransferListener: BoxTransferListener?): File {
        val downloadedFile = blockingDownload(prefix, name, boxTransferListener)
        val outputFile = transferManager.createTempFile()
        decryptFile(key, downloadedFile, outputFile)
        return outputFile
    }

    @Throws(QblStorageException::class)
    override fun blockingUpload(prefix: String, name: String, inputStream: InputStream) {
        try {
            val tmpFile = transferManager.createTempFile()
            IOUtils.copy(inputStream, FileOutputStream(tmpFile))
            blockingUpload(prefix, name, tmpFile, null)
        } catch (e: IOException) {
            throw QblStorageException(e)
        }

    }

    @Throws(QblStorageException::class)
    protected fun blockingUpload(prefix: String, name: String,
                                 file: File, boxTransferListener: BoxTransferListener?): Long {
        val id = transferManager.uploadAndDeleteLocalfileOnSuccess(prefix, name, file, boxTransferListener)
        if (!transferManager.waitFor(id)) {
            throw QblStorageException("Upload failed!")
        }
        return currentSecondsFromEpoch()
    }

    private fun currentSecondsFromEpoch(): Long {
        return System.currentTimeMillis() / 1000
    }

    @Throws(QblStorageException::class)
    private  fun uploadEncrypted(
            content: InputStream, key: KeyParameter, prefix: String, block: String,
            boxTransferListener: BoxTransferListener?): UploadResult {
        try {
            val tempFile = transferManager.createTempFile()
            val outputStream = FileOutputStream(tempFile)
            if (!cryptoUtils.encryptStreamAuthenticatedSymmetric(content, outputStream, key, null)) {
                throw QblStorageException("Encryption failed")
            }
            outputStream.flush()
            val size = tempFile.length()
            val mTime = blockingUpload(prefix, block, tempFile, boxTransferListener)
            return UploadResult(mTime, size)
        } catch (e: IOException) {
            throw QblStorageException(e)
        } catch (e: InvalidKeyException) {
            throw QblStorageException(e)
        }

    }

    @Throws(QblStorageException::class)
    override fun uploadEncrypted(documentIdString: String, content: InputStream): BoxFile {
        val documentId = documentIdParser.parse(documentIdString)

        val key = cryptoUtils.generateSymmetricKey()
        val block = UUID.randomUUID().toString()

        val boxTransferListener = addUploadTransfer(documentId)
        try {
            val uploadResult = uploadEncrypted(content, key, documentId.prefix,
                    BoxManager.BLOCKS_PREFIX + block, boxTransferListener)

            val boxResult = BoxFile(documentId.prefix, block,
                    documentId.fileName, uploadResult.size, uploadResult.mTime, key.key)

            removeUpload(documentIdString, StorageBroadcastConstants.UPLOAD_STATUS_FINISHED, boxResult)
            return boxResult
        } catch (e: QblStorageException) {
            removeUpload(documentIdString, StorageBroadcastConstants.UPLOAD_STATUS_FAILED, null)
            throw e
        } finally {
            notifyBoxChanged()
        }
    }

    @Throws(QblStorageException::class)
    override fun uploadEncrypted(documentIdString: String, content: File): BoxFile {
        try {
            return uploadEncrypted(documentIdString, FileInputStream(content))
        } catch (e: FileNotFoundException) {
            throw QblStorageException(e)
        }

    }

    @Throws(QblStorageException::class)
    override fun uploadEncrypted(prefix: String, block: String, key: ByteArray,
                                 content: InputStream, boxTransferListener: BoxTransferListener?) {
        uploadEncrypted(content, KeyParameter(key), prefix, block, boxTransferListener)
    }

    @Throws(QblStorageException::class)
    override fun delete(prefix: String, ref: String) {
        val requestId = transferManager.delete(prefix, ref)
        this.fileCache.remove(ref)
        if (!transferManager.waitFor(requestId)) {
            throw QblStorageException("Cannot delete file!")
        }
        notifyBoxChanged()
    }

    companion object {

        //TODO Queue is currently not used!
        private val uploadingQueue = LinkedBlockingQueue<BoxUploadingFile>()
        private val cachedFinishedUploads = mutableMapOf<String, MutableMap<String, BoxFile>>()
    }

}
