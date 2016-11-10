package de.qabel.qabelbox.box.notifications

import android.app.NotificationManager
import android.content.Context
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.should.shouldMatch
import de.qabel.qabelbox.*
import de.qabel.qabelbox.base.ACTIVE_IDENTITY
import de.qabel.qabelbox.base.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotification

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class StorageNotificationPresenterTest : UITest {

    override val context: Context
        get() = RuntimeEnvironment.application

    private lateinit var notificationManager: NotificationManager
    private lateinit var presenter: AndroidStorageNotificationPresenter

    companion object {
        private val TEST_OWNER = "owner"
        private val TEST_UPLOAD_PATH = "path"
        private val TEST_FILE_NAME = "FILE"
    }

    @Before
    fun setUp() {
        notificationManager = RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        presenter = AndroidStorageNotificationPresenter(RuntimeEnvironment.application)
        notificationManager.cancelAll()
    }

    private val notifications: List<ShadowNotification>
        get() = shadowOf(notificationManager).allNotifications.map(::shadowOf)

    @Test
    fun testFileIntent() {
        val ownerKey = "thisIsMyFile"
        val path = "/"
        val info = StorageNotificationInfo("test", path, ownerKey, 0L, 0L, 0L)
        val intent = presenter.createFileBrowserIntent(info)
        assertEquals(intent.getStringExtra(ACTIVE_IDENTITY), ownerKey)
        assertEquals(intent.getBooleanExtra(MainActivity.START_FILES_FRAGMENT, false), true)
        assertEquals(intent.getStringExtra(MainActivity.START_FILES_FRAGMENT_PATH), path)
        assertEquals(MainActivity::class.java.name, intent.component.className)
    }

    private fun checkNotification(notification: ShadowNotification, title: String, content: String? = null) {
        notification.contentTitle isEqual title
        content?.let {
            notification.contentText isEqual it
        }
    }

    @Test
    fun testSingleUploadPrepare() {
        val info = StorageNotificationInfo(TEST_FILE_NAME, TEST_UPLOAD_PATH, TEST_OWNER, 1L, 2L, 3L)
        presenter.showEncryptingUploadNotification(info)
        val notifications = notifications
        notifications shouldMatch hasSize(equalTo(1))
        checkNotification(notifications[0], title = getString(R.string.uploading, TEST_FILE_NAME),
                content = getString(R.string.encrypting))
        notifications[0].progressBar.isIndeterminate isEqual true
    }

    @Test
    fun testSingleUploadLoading() {
        val info = StorageNotificationInfo(TEST_FILE_NAME, TEST_UPLOAD_PATH, TEST_OWNER, 1L, 2L, 3L)
        presenter.showUploadProgressNotification(info)
        val notifications = notifications
        notifications shouldMatch hasSize(equalTo(1))
        checkNotification(notifications[0], title = getString(R.string.uploading, TEST_FILE_NAME))
        notifications[0].progressBar.progress isEqual (100 * 2 / 3)
    }

    @Test
    fun testSingleUploadCompleted() {
        val info = StorageNotificationInfo(TEST_FILE_NAME, TEST_UPLOAD_PATH, TEST_OWNER, 1L, 2L, 3L)
        presenter.showUploadCompletedNotification(info)
        val notifications = notifications
        notifications shouldMatch hasSize(equalTo(1))
        checkNotification(notifications[0], title = getString(R.string.upload_complete_title),
                content = getString(R.string.upload_complete_msg, TEST_FILE_NAME))
    }

    @Test
    fun testSingleUploadFailed() {
        val info = StorageNotificationInfo(TEST_FILE_NAME, TEST_UPLOAD_PATH, TEST_OWNER, 1L, 2L, 3L)
        presenter.showEncryptingUploadNotification(info)
        val notifications = notifications
        notifications shouldMatch hasSize(equalTo(1))
        checkNotification(notifications[0], title = getString(R.string.uploading, TEST_FILE_NAME),
                content = getString(R.string.encrypting))
    }

    @Test
    fun testMultipleUpload() {
        val info = StorageNotificationInfo(TEST_FILE_NAME, TEST_UPLOAD_PATH, TEST_OWNER, 1L, 2L, 3L)
        presenter.showUploadProgressNotification(info)

        val info2 = StorageNotificationInfo("File 2", TEST_UPLOAD_PATH, TEST_OWNER, 1L, 2L, 3L)
        presenter.showUploadFailedNotification(info2)

        //tests one per file
        presenter.showUploadProgressNotification(info)

        val notifications = notifications
        notifications shouldMatch hasSize(equalTo(2))

        assert(notifications.all {
            it.contentTitle == getString(R.string.uploading, TEST_FILE_NAME)
                    || it.contentTitle == getString(R.string.upload_failed_title)
        })
    }

    @Test
    fun testSingleDownloadLoading() {
        val info = StorageNotificationInfo(TEST_FILE_NAME, TEST_UPLOAD_PATH, TEST_OWNER, 1L, 2L, 3L)
        presenter.showDownloadProgressNotification(info)
        val notifications = notifications
        notifications shouldMatch hasSize(equalTo(1))
        checkNotification(notifications[0], title = getString(R.string.downloading, TEST_FILE_NAME))
        notifications[0].progressBar.progress isEqual (100 * 2 / 3)
    }

    @Test
    fun testSingleDownloadDecrypting() {
        val info = StorageNotificationInfo(TEST_FILE_NAME, TEST_UPLOAD_PATH, TEST_OWNER, 1L, 2L, 3L)
        presenter.showDecryptingDownloadNotification(info)
        val notifications = notifications
        notifications shouldMatch hasSize(equalTo(1))
        checkNotification(notifications[0], title = getString(R.string.downloading, TEST_FILE_NAME),
                content = getString(R.string.decrypting))
        notifications[0].progressBar.isIndeterminate isEqual true
    }

    @Test
    fun testSingleDownloadCompleted() {
        val info = StorageNotificationInfo(TEST_FILE_NAME, TEST_UPLOAD_PATH, TEST_OWNER, 1L, 2L, 3L)
        presenter.showDownloadCompletedNotification(info)
        val notifications = notifications
        notifications shouldMatch hasSize(equalTo(1))
        checkNotification(notifications[0], title = getString(R.string.download_complete),
                content = getString(R.string.download_complete_msg, TEST_FILE_NAME))
    }

    @Test
    fun testSingleDownloadFailed() {
        val info = StorageNotificationInfo(TEST_FILE_NAME, TEST_UPLOAD_PATH, TEST_OWNER, 1L, 2L, 3L)
        presenter.showDownloadFailedNotification(info)
        val notifications = notifications
        notifications shouldMatch hasSize(equalTo(1))
        checkNotification(notifications[0], title = getString(R.string.download_failed),
                content = getString(R.string.download_failed_msg, TEST_FILE_NAME))
    }

    @Test
    fun testMultipleDownloadUpload() {
        val info = StorageNotificationInfo(TEST_FILE_NAME, TEST_UPLOAD_PATH, TEST_OWNER, 1L, 2L, 3L)
        presenter.showDownloadProgressNotification(info)

        val info2 = StorageNotificationInfo("File 2", TEST_UPLOAD_PATH, TEST_OWNER, 1L, 2L, 3L)
        presenter.showDownloadFailedNotification(info2)

        //Recall
        presenter.showDownloadProgressNotification(info)

        val notifications = notifications
        notifications shouldMatch hasSize(equalTo(2))

        assert(notifications.all {
            it.contentTitle == getString(R.string.downloading, TEST_FILE_NAME)
                    || it.contentTitle == getString(R.string.download_failed)
        })
    }
}
