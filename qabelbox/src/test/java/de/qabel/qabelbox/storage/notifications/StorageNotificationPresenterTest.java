package de.qabel.qabelbox.storage.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowNotificationManager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.storage.model.BoxUploadingFile;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class StorageNotificationPresenterTest {

    private static final String TEST_OWNER = "owner";
    private static final String TEST_UPLOAD_PATH = "path";
    private static final String TEST_FILE_NAME = "FILE";

    private NotificationManager notificationManager;
    private AndroidStorageNotificationPresenter presenter;

    @Before
    public void setUp() {
        this.notificationManager = (NotificationManager)
                RuntimeEnvironment.application.getSystemService(Context.NOTIFICATION_SERVICE);
        presenter = new AndroidStorageNotificationPresenter(RuntimeEnvironment.application);
    }

    private List<ShadowNotification> getNotifications() {
        ShadowNotificationManager shadowManger = shadowOf(notificationManager);
        for (Notification n : shadowManger.getAllNotifications()) {
            Log.d("LOG", shadowOf(n).getContentTitle().toString());

        }
        List<Notification> notifications =  shadowManger.getAllNotifications();
        List<ShadowNotification> shadows = new ArrayList<>(notifications.size());
        for(Notification n : notifications){
            shadows.add(shadowOf(n));
        }
        return shadows;

    }

    @Test
    public void testFileIntent() {
        String ownerKey = "thisIsMyFile";
        String path = "/";
        StorageNotificationInfo info = new StorageNotificationInfo("test", path, ownerKey, 0L, 0L);
        Intent intent = presenter.createFileIntent(info);
        assertEquals(intent.getStringExtra(MainActivity.ACTIVE_IDENTITY), ownerKey);
        assertEquals(intent.getBooleanExtra(MainActivity.START_FILES_FRAGMENT, false), true);
        assertEquals(intent.getStringExtra(MainActivity.START_FILES_FRAGMENT_PATH), path);
        assertEquals(MainActivity.class.getName(), intent.getComponent().getClassName());
    }

    private void checkUploadCompleteNotification(String lastname) {
        String title = RuntimeEnvironment.application.getString(R.string.upload_complete_notification_title);
        String expectedContent = String.format(RuntimeEnvironment.application.getString(R.string.upload_complete_notification_msg), lastname);
        ShadowNotification shadowNotification = getNotifications().get(0);
        checkNotification(shadowNotification, title, expectedContent);
    }

    private void checkSinglePendingUploadNotification(StorageNotificationInfo info) {
        ShadowNotification shadowNotification = getNotifications().get(0);

        int progress = info.getProgress();
        assertEquals(progress,
                shadowNotification.getProgressBar().getProgress());

        String expectedTitle = RuntimeEnvironment.application.getResources().
                getQuantityString(R.plurals.uploadsNotificationTitle, 1,
                        info.getFileName());

        String expectedContent = String.format(RuntimeEnvironment.application.getString(
                R.string.upload_in_progress_notification_content),
                progress + "%");

        checkNotification(shadowNotification, expectedTitle, expectedContent);
    }

    private void checkMultiplePendingUploads(StorageNotificationInfo currentUpload, int queued) {
        ShadowNotification shadowNotification = getNotifications().get(0);

        assertEquals(currentUpload.getProgress(),
                shadowNotification.getProgressBar().getProgress());

        String expectedTitle = RuntimeEnvironment.application.getResources().
                getQuantityString(R.plurals.uploadsNotificationTitle, queued,
                        queued);

        String expectedContent = String.format(RuntimeEnvironment.application.getString(
                R.string.upload_in_progress_notification_content),
                currentUpload.getProgress() + "%");

        checkNotification(shadowNotification, expectedTitle, expectedContent);
    }

    private void checkDownloadProgressNotification(ShadowNotification notification, StorageNotificationInfo info) {
        String title = String.format(RuntimeEnvironment.application.
                getString(R.string.downloading), info.getFileName());
        String content = FileUtils.byteCountToDisplaySize(info.getDoneBytes()) + " / " + FileUtils.byteCountToDisplaySize(info.getTotalBytes());
        checkNotification(notification, title, content);

        assertEquals(info.getProgress(), notification.getProgressBar().getProgress());
    }

    private void checkDownloadCompleteNotification(ShadowNotification notification, String name) {
        String title = RuntimeEnvironment.application.
                getString(R.string.download_complete);
        String content = String.format(RuntimeEnvironment.application.
                getString(R.string.download_complete_msg), name);
        checkNotification(notification, title, content);
    }

    private void checkNotification(ShadowNotification notification, String title, String content) {
        assertEquals(title, notification.getContentTitle());
        assertEquals(content, notification.getContentText());
    }

    @Test
    public void testSingleUpload() {
        BoxUploadingFile uploadingFile = new BoxUploadingFile(TEST_FILE_NAME, TEST_UPLOAD_PATH, TEST_OWNER);
        Queue<BoxUploadingFile> queue = new LinkedList<>();
        queue.add(uploadingFile);
        StorageNotificationInfo info = new StorageNotificationInfo(TEST_FILE_NAME, TEST_UPLOAD_PATH, TEST_OWNER, 0, 1);
        presenter.updateUploadNotification(queue.size(), info);
        checkSinglePendingUploadNotification(info);

        info.setProgress(100, 200);
        presenter.updateUploadNotification(queue.size(), info);
        checkSinglePendingUploadNotification(info);

        queue.poll();
        info.complete();
        presenter.updateUploadNotification(queue.size(), info);
        checkUploadCompleteNotification(info.getFileName());
    }

    @Test
    public void testMultipleUpload() {

        long size = 200L;

        Queue<BoxUploadingFile> queue = new LinkedList<>();
        queue.add(new BoxUploadingFile("FILE", TEST_UPLOAD_PATH, TEST_OWNER));
        queue.add(new BoxUploadingFile("FILE2", TEST_UPLOAD_PATH, TEST_OWNER));
        queue.add(new BoxUploadingFile("FILE3", TEST_UPLOAD_PATH, TEST_OWNER));

        StorageNotificationInfo info = new StorageNotificationInfo("FILE",
                TEST_UPLOAD_PATH, TEST_OWNER, 0L, 1L);
        //3 Uploads
        presenter.updateUploadNotification(queue.size(), info);
        checkMultiplePendingUploads(info, queue.size());

        //2 Uploads
        queue.poll();
        long progress = 100L;
        BoxUploadingFile current = queue.peek();
        current.totalSize = size;
        current.uploadedSize = progress;
        info = new StorageNotificationInfo("FILE2", TEST_UPLOAD_PATH, TEST_OWNER,
                progress, size);
        presenter.updateUploadNotification(queue.size(), info);

        //1 Upload
        queue.poll();
        info = new StorageNotificationInfo("FILE3", TEST_UPLOAD_PATH, TEST_OWNER, 0L, 1L);
        presenter.updateUploadNotification(queue.size(), info);

        //Uploads complete
        BoxUploadingFile lastFile = queue.poll();
        lastFile.uploadedSize = size;
        lastFile.totalSize = size;
        info = new StorageNotificationInfo("FILE3", TEST_UPLOAD_PATH, TEST_OWNER, size, size);
        presenter.updateUploadNotification(queue.size(), info);
        checkUploadCompleteNotification("FILE3");
    }

    @Test
    public void testSingleDownload() {
        String ownerKey = "thisIsMyFile";
        String path = "/";
        long size = 4048L;
        StorageNotificationInfo info = new StorageNotificationInfo(
                "testfile", path, ownerKey, 0L, size);
        presenter.updateDownloadNotification(info);
        ShadowNotification notification = getNotifications().get(0);
        checkDownloadProgressNotification(notification, info);

        //Check Progress
        long progress = size / 2;
        info.setProgress(progress, size);
        presenter.updateDownloadNotification(info);
        notification = getNotifications().get(0);
        checkDownloadProgressNotification(notification, info);

        progress += size / 3;
        info.setProgress(progress, size);
        presenter.updateDownloadNotification(info);
        notification = getNotifications().get(0);
        checkDownloadProgressNotification(notification, info);

        info.complete();
        presenter.updateDownloadNotification(info);
        notification = getNotifications().get(0);
        checkDownloadCompleteNotification(notification, info.getFileName());
    }

    @Test
    public void testMultiDownload() {
        String ownerKey = "thisIsMyFile";
        String path = "/";
        long sizeA = 4048L;
        long sizeB = 4048L * 2;

        StorageNotificationInfo expectedInfoA = new StorageNotificationInfo(
                "testfile", path, ownerKey, 0L, sizeA);

        StorageNotificationInfo expectedInfoB = new StorageNotificationInfo(
                "testfile2", path, ownerKey, 0L, sizeB);

        presenter.updateDownloadNotification(expectedInfoA);
        presenter.updateDownloadNotification(expectedInfoB);
        List<ShadowNotification> notifications = getNotifications();
        long foundCurrent = 0;
        for(ShadowNotification n : notifications){
            try{
                checkDownloadProgressNotification(n, expectedInfoA);
            }catch (AssertionError e){
                checkDownloadProgressNotification(n, expectedInfoB);
            }
            foundCurrent++;
        }
        assertEquals(2, foundCurrent);
    }
}
