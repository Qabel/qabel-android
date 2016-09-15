package de.qabel.qabelbox.box.backends

import de.qabel.box.storage.*
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.box.storage.exceptions.QblStorageNotFound
import de.qabel.qabelbox.communication.callbacks.DownloadRequestCallback
import de.qabel.qabelbox.communication.callbacks.RequestCallback
import de.qabel.qabelbox.communication.callbacks.UploadRequestCallback
import de.qabel.qabelbox.storage.server.BlockServer
import okhttp3.Response
import org.apache.http.client.utils.DateUtils
import java.io.File
import java.io.InputStream
import java.util.*
import java.util.concurrent.CountDownLatch

class BoxHttpStorageBackend (
        private val blockServer: BlockServer,
        private val prefix: String):
        StorageReadBackend, StorageWriteBackend {
    override fun getUrl(name: String): String = blockServer.urlForFile(prefix, name)


    data class Response(val eTag: String?, val status: Int, val date: Date?, val error: Exception?)

    override fun download(name: String): StorageDownload {
        return download(name, null)
    }

    fun downloadRequest(file: File, name: String, ifModified: String?): Response {
        val latch = CountDownLatch(1)
        var error: Exception? = null
        var etag: String? = null
        var status: Int = 0
        var date: Date? = null
        blockServer.downloadFile(prefix, name, ifModified, object : DownloadRequestCallback(file) {
            override fun onSuccess(statusCode: Int, response: okhttp3.Response?) {
                super.onSuccess(statusCode, response)
                etag = response?.header("Etag")
                date = DateUtils.parseDate(response?.header("Date"))
                status = statusCode
                latch.countDown()
            }
            override fun onError(e: Exception?, response: okhttp3.Response?) {
                error = e
                status = response?.code() ?: 0
                latch.countDown()
            }

        })
        latch.await()
        return Response(etag, status, date, error)
    }

    fun handleDownloadResponse(response: Response, file: File): StorageDownload {
        val (eTag, status, date, error) = response
        when (status) {
            0 -> throw QblStorageException("Download failed")
            404 -> throw QblStorageNotFound("Not found")
            503 -> throw QblStorageNotFound("Forbidden")
            304 -> throw UnmodifiedException()
        }
        error?.let { throw QblStorageException(it) }
        return StorageDownload(file.inputStream(), eTag, file.length())
    }

    override fun download(name: String, ifModified: String?): StorageDownload {
        val file = createTempFile()
        val response = downloadRequest(file, name, ifModified)
        return handleDownloadResponse(response, file)
    }

    fun uploadRequest(file: File, name: String, etag: String?): Response {
        val latch = CountDownLatch(1)
        var error: Exception? = null
        var status: Int = 0
        var responseEtag: String? = null
        var date: Date? = null
        blockServer.uploadFile(prefix, name, file, etag, object: UploadRequestCallback(200, 204, 304) {
            override fun onSuccess(statusCode: Int, response: okhttp3.Response?) {
                responseEtag = response?.header("ETag")
                status = statusCode
                date = DateUtils.parseDate(response?.header("Date"))
                latch.countDown()
            }

            override fun onError(e: Exception?, response: okhttp3.Response?) {
                error = e
                status = response?.code() ?: 0
                latch.countDown()
            }

            override fun onProgress(currentBytes: Long, totalBytes: Long) {
            }

        })
        latch.await()
        return Response(responseEtag, status, date, error)
    }

    fun handleUploadResponse(response: Response): StorageWriteBackend.UploadResult {
        val (eTag, status, date, error) = response
        when (status) {
            0 -> throw QblStorageException("Upload failed")
            401 -> throw QblStorageException("Permission denied")
            412 -> throw ModifiedException("The target file was already changed")
            403, 404 -> QblStorageNotFound("File not found")
        }
        error?.let { throw QblStorageException(it) }
        return StorageWriteBackend.UploadResult(Date(), eTag ?: "")
    }

    override fun upload(name: String, content: InputStream, eTag: String?): StorageWriteBackend.UploadResult {
        val file = createTempFile()
        file.outputStream().use { content.copyTo(it) }
        val response = uploadRequest(file, name, eTag)
        return handleUploadResponse(response)
    }

    override fun upload(name: String, content: InputStream): StorageWriteBackend.UploadResult {
        return upload(name, content, null);
    }


    fun deleteRequest(name: String): Response {
        val latch = CountDownLatch(1)
        var error: Exception? = null
        var status: Int = 0
        blockServer.deleteFile(prefix, name, object : RequestCallback(204, 200, 404) {
            override fun onSuccess(statusCode: Int, response: okhttp3.Response?) {
                status = statusCode
                latch.countDown()
            }
            override fun onError(e: Exception?, response: okhttp3.Response?) {
                error = e
                latch.countDown()
            }

        })
        latch.await()
        return Response(null, status, null, error)
    }

    fun handleDeleteResponse(response: Response) {
        val (eTag, status, date, error) = response
        when (status) {
            401 -> throw QblStorageNotFound("Forbidden")
        }
        error?.let { throw QblStorageException(it) }
    }

    override fun delete(name: String) {
        val response = deleteRequest(name)
        handleDeleteResponse(response)
    }

}

