package de.qabel.qabelbox.box.backends


import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.should.shouldMatch
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import de.qabel.box.storage.UnmodifiedException
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.box.storage.exceptions.QblStorageNotFound
import de.qabel.qabelbox.isEqual
import de.qabel.qabelbox.storage.server.AndroidBlockServer
import org.junit.Before
import org.junit.Test

class BoxHttpStorageBackendTest {

    val prefix = "prefix"
    lateinit var backend: BoxHttpStorageBackend
    lateinit var server: AndroidBlockServer
    val response = BoxHttpStorageBackend.Response(null, 0, null)

    @Before
    fun mockedServer() {
        server = mock()
        backend = BoxHttpStorageBackend(server, prefix)
    }

    @Test
    fun getUrl() {
        val result: String = "url"
        whenever(server.urlForFile(prefix, "name")).thenReturn(result)

        backend.getUrl("name") isEqual result
    }

    @Test(expected = QblStorageException::class)
    fun downloadWithInvalidStatus() {
        backend.handleDownloadResponse(response, mock())
    }

    @Test(expected = QblStorageNotFound::class)
    fun downloadNotFound() {
        backend.handleDownloadResponse(response.copy(status = 404), mock())
    }

    @Test(expected = QblStorageNotFound::class)
    fun downloadForbidden() {
        backend.handleDownloadResponse(response.copy(status = 503), mock())
    }

    @Test(expected = UnmodifiedException::class)
    fun downloadUnmodified() {
        backend.handleDownloadResponse(response.copy(status = 304), mock())
    }

    @Test(expected = QblStorageException::class)
    fun downloadWrapsException() {
        backend.handleDownloadResponse(response.copy(
                status = 200, error = IllegalArgumentException()), mock())
    }

    @Test(expected = QblStorageException::class)
    fun uploadFailed() {
        backend.handleUploadResponse(response)
    }

    @Test(expected = QblStorageException::class)
    fun uploadDenied() {
        backend.handleUploadResponse(response.copy(status = 401))
    }

    @Test(expected = QblStorageException::class)
    fun uploadWrapsException() {
        backend.handleUploadResponse(response.copy(
                status = 200, error = IllegalArgumentException()))
    }

    @Test(expected = QblStorageException::class)
    fun deleteDenied() {
        backend.handleDeleteResponse(response.copy(status = 401))
    }

    @Test(expected = QblStorageException::class)
    fun deleteWrapsException() {
        backend.handleDeleteResponse(response.copy(
                status = 200, error = IllegalArgumentException()))
    }
}
