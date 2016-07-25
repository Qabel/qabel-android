package de.qabel.qabelbox.box.backends

import de.qabel.box.storage.StorageDownload
import de.qabel.box.storage.StorageReadBackend
import de.qabel.box.storage.StorageWriteBackend
import de.qabel.box.storage.UnmodifiedException
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.box.storage.exceptions.QblStorageNotFound
import de.qabel.qabelbox.communication.callbacks.DownloadRequestCallback
import de.qabel.qabelbox.communication.callbacks.RequestCallback
import de.qabel.qabelbox.communication.callbacks.UploadRequestCallback
import de.qabel.qabelbox.storage.server.BlockServer
import okhttp3.Response
import java.io.InputStream
import java.util.concurrent.CountDownLatch

class BoxHttpStorageBackend (
        private val blockServer: BlockServer,
        private val prefix: String):
        StorageReadBackend, StorageWriteBackend {

    override fun getUrl(name: String): String = blockServer.urlForFile(prefix, name)

    override fun download(name: String): StorageDownload {
        return blockingDownload(name)
    }

    override fun download(name: String, ifModified: String): StorageDownload {
        return blockingDownload(name, ifModified)
    }

    private fun blockingDownload(name: String, ifModified: String? = null): StorageDownload {
        val latch = CountDownLatch(1)
        val file = createTempFile()
        var error: Exception? = null
        var etag: String? = null
        var status: Int = 0
        blockServer.downloadFile(prefix, name, ifModified, object : DownloadRequestCallback(file) {
            override fun onSuccess(statusCode: Int, response: Response?) {
                super.onSuccess(statusCode, response)
                etag = response?.header("Etag")
                status = statusCode
                latch.countDown()
            }
            override fun onError(e: Exception?, response: Response?) {
                error = e
                status = response?.code() ?: 0
                latch.countDown()
            }

        })
        latch.await()
        when (status) {
            0 -> throw QblStorageException("Download failed")
            404 -> throw QblStorageNotFound("Not found")
            503 -> throw QblStorageNotFound("Forbidden")
            304 -> throw UnmodifiedException()
        }
        error?.let { throw QblStorageException(it) }
        return StorageDownload(file.inputStream(), etag, file.length())
    }

    override fun upload(name: String, content: InputStream): Long {
        val latch = CountDownLatch(1)
        val file = createTempFile()
        file.outputStream().use { content.copyTo(it) }
        var error: Exception? = null
        var status: Int = 0
        var eTag: String? = null
        blockServer.uploadFile(prefix, name, file, object: UploadRequestCallback(200, 204, 304) {
            override fun onSuccess(statusCode: Int, response: Response?) {
                eTag = response?.header("ETag")
                status = statusCode
                latch.countDown()
            }

            override fun onError(e: Exception?, response: Response?) {
                error = e
                status = response?.code() ?: 0
                latch.countDown()
            }

            override fun onProgress(currentBytes: Long, totalBytes: Long) {
            }

        })
        latch.await()
        when (status) {
            0 -> throw QblStorageException("Upload failed")
            404 -> throw QblStorageNotFound("Not found")
        }
        error?.let { throw QblStorageException(it) }
        eTag?.let {
            try {
                return it.toLong()
            } catch (ignored: NumberFormatException) {

            }
        }
        return System.currentTimeMillis()
    }

    override fun delete(name: String) {
        val latch = CountDownLatch(1)
        var error: Exception? = null
        var status: Int = 0
        blockServer.deleteFile(prefix, name, object : RequestCallback() {
            override fun onSuccess(statusCode: Int, response: Response?) {
                status = statusCode
                latch.countDown()
            }
            override fun onError(e: Exception?, response: Response?) {
                error = e
                latch.countDown()
            }

        })
        latch.await()
        when (status) {
            0 -> throw QblStorageException("Download failed")
            503 -> throw QblStorageNotFound("Forbidden")
        }
        error?.let { throw QblStorageException(it) }
    }

}

