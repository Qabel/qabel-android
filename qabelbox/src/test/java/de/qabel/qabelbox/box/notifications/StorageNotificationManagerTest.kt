package de.qabel.qabelbox.box.notifications

import android.app.NotificationManager
import android.content.Context
import de.qabel.box.storage.dto.BoxPath
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.box.dto.FileOperationState
import de.qabel.qabelbox.box.interactor.BoxFileBrowser
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class StorageNotificationManagerTest {

    private lateinit var notificationManager: NotificationManager
    private lateinit var storageNotificationManager: StorageNotificationManager
    private lateinit var fakePresenter: StorageNotificationPresenter

    companion object {
        private val TEST_OWNER = "owner"
        private val TEST_KEYS = BoxFileBrowser.KeyAndPrefix(TEST_OWNER, "")
        private val TEST_UPLOAD_PATH = "path"
        private val TEST_FILE_NAME = "FILE"
    }

    @Before
    fun setUp() {
        notificationManager = RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        fakePresenter = mock(StorageNotificationPresenter::class.java)
        storageNotificationManager = AndroidStorageNotificationManager(fakePresenter)
    }

    @Test
    fun testSingleUpload() {
        val expectedInfo = StorageNotificationInfo(TEST_FILE_NAME, "/" + TEST_UPLOAD_PATH, TEST_OWNER, 100L, 0, 1)
        val operation = FileOperationState(TEST_KEYS, TEST_FILE_NAME, BoxPath.Root / TEST_UPLOAD_PATH, 100L, 0, 1)

        storageNotificationManager.updateUploadNotification(operation)
        verify(fakePresenter).showEncryptingUploadNotification(expectedInfo)

        operation.status = FileOperationState.Status.COMPLETE
        storageNotificationManager.updateUploadNotification(operation)
        verify(fakePresenter).showUploadCompletedNotification(expectedInfo)
    }

    @Test
    fun testMultipleUpload() {
        val expectedInfo = StorageNotificationInfo(TEST_FILE_NAME, "/" + TEST_UPLOAD_PATH, TEST_OWNER, 100L, 0, 1)
        val operation = FileOperationState(TEST_KEYS, TEST_FILE_NAME, BoxPath.Root / TEST_UPLOAD_PATH, 100L, 0, 1).apply {
            status = FileOperationState.Status.LOADING
        }

        val expectedInfo2 = StorageNotificationInfo(TEST_FILE_NAME, "/" + TEST_UPLOAD_PATH, TEST_OWNER, 100L, 0, 1)
        val operation2 = FileOperationState(TEST_KEYS, TEST_FILE_NAME, BoxPath.Root / TEST_UPLOAD_PATH, 100L, 0, 1).apply {
            status = FileOperationState.Status.ERROR
        }

        storageNotificationManager.updateUploadNotification(operation)
        storageNotificationManager.updateUploadNotification(operation2)
        verify(fakePresenter).showUploadProgressNotification(expectedInfo)
        verify(fakePresenter).showUploadFailedNotification(expectedInfo2)
    }

    @Test
    fun testSingleDownload() {
        val expectedInfo = StorageNotificationInfo(TEST_FILE_NAME, "/" + TEST_UPLOAD_PATH, TEST_OWNER, 100L, 0, 1)
        val operation = FileOperationState(TEST_KEYS, TEST_FILE_NAME, BoxPath.Root / TEST_UPLOAD_PATH, 100L, 0, 1)

        storageNotificationManager.updateDownloadNotification(operation)
        verify(fakePresenter).showDownloadProgressNotification(expectedInfo)

        operation.status = FileOperationState.Status.COMPLETING
        storageNotificationManager.updateDownloadNotification(operation)
        verify(fakePresenter).showDecryptingDownloadNotification(expectedInfo)
    }

    @Test
    fun testMultiDownload() {
        val expectedInfo = StorageNotificationInfo(TEST_FILE_NAME, "/" + TEST_UPLOAD_PATH, TEST_OWNER, 100L, 0, 1)
        val operation = FileOperationState(TEST_KEYS, TEST_FILE_NAME, BoxPath.Root / TEST_UPLOAD_PATH, 100L, 0, 1).apply {
            status = FileOperationState.Status.LOADING
        }

        val expectedInfo2 = StorageNotificationInfo(TEST_FILE_NAME, "/" + TEST_UPLOAD_PATH, TEST_OWNER, 100L, 0, 1)
        val operation2 = FileOperationState(TEST_KEYS, TEST_FILE_NAME, BoxPath.Root / TEST_UPLOAD_PATH, 100L, 0, 1).apply {
            status = FileOperationState.Status.ERROR
        }

        storageNotificationManager.updateDownloadNotification(operation)
        storageNotificationManager.updateDownloadNotification(operation2)
        verify(fakePresenter).showDownloadProgressNotification(expectedInfo)
        verify(fakePresenter).showDownloadFailedNotification(expectedInfo2)
    }

}
