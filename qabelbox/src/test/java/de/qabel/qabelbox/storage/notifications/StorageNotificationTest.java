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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.storage.model.BoxFile;
import de.qabel.qabelbox.storage.transfer.BoxTransferListener;
import de.qabel.qabelbox.storage.model.BoxUploadingFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class StorageNotificationTest {

    private static final String TEST_OWNER = "owner";
    private static final String TEST_UPLOAD_PATH = "path";
    private static final String TEST_FILE_NAME = "FILE";

    private NotificationManager notificationManager;
    private StorageNotificationManager storageNotificationManager;
    private StorageNotificationPresenter fakePresenter;

    @Before
    public void setUp() {
        notificationManager = (NotificationManager) RuntimeEnvironment.application
                .getSystemService(Context.NOTIFICATION_SERVICE);
        fakePresenter = mock(StorageNotificationPresenter.class);
        storageNotificationManager = new AndroidStorageNotificationManager(fakePresenter);
    }

    @Test
    public void testSingleUpload() {
        BoxUploadingFile uploadingFile = new BoxUploadingFile(TEST_FILE_NAME, TEST_UPLOAD_PATH, TEST_OWNER);
        Queue<BoxUploadingFile> queue = new LinkedList<>();
        queue.add(uploadingFile);
        StorageNotificationInfo expectedInfo = new StorageNotificationInfo(TEST_FILE_NAME, TEST_UPLOAD_PATH, TEST_OWNER, 0, 0);
        storageNotificationManager.updateUploadNotification(queue.size(), uploadingFile);
        verify(fakePresenter).updateUploadNotification(1, expectedInfo);

        queue.poll();
        storageNotificationManager.updateUploadNotification(queue.size(), queue.peek());
        verify(fakePresenter).updateUploadNotification(0, null);
    }
/*
    TODO redesign
    @Test
    public void testMultipleUpload() {
        Queue<BoxUploadingFile> queue = new LinkedList<>();
        queue.add(new BoxUploadingFile("FILE", TEST_UPLOAD_PATH, TEST_OWNER));
        queue.add(new BoxUploadingFile("FILE2", TEST_UPLOAD_PATH, TEST_OWNER));
        queue.add(new BoxUploadingFile("FILE3", TEST_UPLOAD_PATH, TEST_OWNER));
        //3 Uploads
        storageNotificationManager.updateUploadNotification(queue);
        checkMultiplePendingUploads(queue.peek(), queue.size());

        //2 Uploads
        queue.poll();
        storageNotificationManager.updateUploadNotification(queue);
        checkMultiplePendingUploads(queue.peek(), queue.size());

        //1 Upload
        queue.poll();
        storageNotificationManager.updateUploadNotification(queue);
        checkSinglePendingUploadNotification(queue.peek());

        //Uploads complete
        BoxUploadingFile lastFile = queue.poll();
        storageNotificationManager.updateUploadNotification(queue);
        checkUploadCompleteNotification(lastFile);
    }

    @Test
    public void testSingleDownload() {
        String ownerKey = "thisIsMyFile";
        String path = "/";
        BoxFile file = new BoxFile(TestConstants.PREFIX, "block", "testfile", 4048L, System.currentTimeMillis(), new byte[]{0x01, 0x02});
        BoxTransferListener listener = storageNotificationManager.addDownloadNotifications(ownerKey, path, file);

        //Check initial
        ShadowNotification notification = getNotification(0, 1);
        checkDownloadProgressNotification(notification, file, 0, file.size);

        //Check Progress
        long progress = file.size / 2;
        listener.onProgressChanged(progress, file.size);
        notification = getNotification(0, 1);
        checkDownloadProgressNotification(notification, file, progress, file.size);

        progress += file.size / 3;
        listener.onProgressChanged(progress, file.size);
        notification = getNotification(0, 1);
        checkDownloadProgressNotification(notification, file, progress, file.size);

        listener.onFinished();
        notification = getNotification(0, 1);
        checkDownloadCompleteNotification(notification, file);
    }

    @Test
    public void testMultiDownload() {
        String ownerKey = "thisIsMyFile";
        String path = "/";
        Map<BoxFile, BoxTransferListener> fileMap = new HashMap<>();

        BoxFile fileA = new BoxFile(TestConstants.PREFIX, "block", "testfile", 4048L, System.currentTimeMillis(), new byte[]{0x01, 0x02});
        BoxTransferListener listenerA = storageNotificationManager.addDownloadNotifications(ownerKey, path, fileA);
        BoxFile fileB = new BoxFile(TestConstants.PREFIX, "block2", "testfile2", 4048L, System.currentTimeMillis(), new byte[]{0x01, 0x02});
        BoxTransferListener listenerB = storageNotificationManager.addDownloadNotifications(ownerKey, path, fileB);

        listenerA.onProgressChanged(50, fileA.size);
        listenerB.onProgressChanged(fileB.size, fileB.size);

        checkDownloadCompleteNotification(getNotification(1, 2), fileB);
        checkDownloadProgressNotification(getNotification(0, 2), fileA, 50, fileA.size);
    }

    @Test
    public void testFileIntent() {
        String ownerKey = "thisIsMyFile";
        String path = "/";
        Intent intent = storageNotificationManager.createFileIntent(ownerKey, path);
        assertEquals(intent.getStringExtra(MainActivity.ACTIVE_IDENTITY), ownerKey);
        assertEquals(intent.getBooleanExtra(MainActivity.START_FILES_FRAGMENT, false), true);
        assertEquals(intent.getStringExtra(MainActivity.START_FILES_FRAGMENT_PATH), path);
        assertEquals(MainActivity.class.getName(), intent.getComponent().getClassName());
    }

    private ShadowNotification getNotification(int index, int expectedSize) {
        ShadowNotificationManager shadowManger = shadowOf(notificationManager);
        assertEquals(expectedSize, shadowManger.size());
        for (Notification n : shadowManger.getAllNotifications()) {
            Log.d("LOG", shadowOf(n).getContentTitle().toString());
        }
        Notification notification = shadowManger.getAllNotifications().get(index);
        ShadowNotification shadowNotification = shadowOf(notification);
        assertNotNull(shadowNotification);
        return shadowNotification;
    }

    private void checkUploadCompleteNotification(BoxUploadingFile lastItem) {
        String title = RuntimeEnvironment.application.getString(R.string.upload_complete_notification_title);
        String expectedContent = String.format(RuntimeEnvironment.application.getString(R.string.upload_complete_notification_msg), lastItem.name);
        ShadowNotification shadowNotification = getNotification(0, 1);
        checkNotification(shadowNotification, title, expectedContent);
    }

    private void checkSinglePendingUploadNotification(BoxUploadingFile currentUpload) {
        ShadowNotification shadowNotification = getNotification(0, 1);

        assertEquals(currentUpload.getUploadStatusPercent(),
                shadowNotification.getProgressBar().getProgress());

        String expectedTitle = RuntimeEnvironment.application.getResources().
                getQuantityString(R.plurals.uploadsNotificationTitle, 1,
                        currentUpload.name);

        String expectedContent = String.format(RuntimeEnvironment.application.getString(
                R.string.upload_in_progress_notification_content),
                currentUpload.getUploadStatusPercent() + "%");

        checkNotification(shadowNotification, expectedTitle, expectedContent);
    }

    private void checkMultiplePendingUploads(BoxUploadingFile currentUpload, int queued) {
        ShadowNotification shadowNotification = getNotification(0, 1);

        assertEquals(currentUpload.getUploadStatusPercent(),
                shadowNotification.getProgressBar().getProgress());

        String expectedTitle = RuntimeEnvironment.application.getResources().
                getQuantityString(R.plurals.uploadsNotificationTitle, queued,
                        queued);

        String expectedContent = String.format(RuntimeEnvironment.application.getString(
                R.string.upload_in_progress_notification_content),
                currentUpload.getUploadStatusPercent() + "%");

        checkNotification(shadowNotification, expectedTitle, expectedContent);
    }

    private void checkDownloadProgressNotification(ShadowNotification notification, BoxFile file, long progress, long size) {
        String title = String.format(RuntimeEnvironment.application.
                getString(R.string.downloading), file.name);
        String content = FileUtils.byteCountToDisplaySize(progress) + " / " + FileUtils.byteCountToDisplaySize(size);
        checkNotification(notification, title, content);

        assertEquals(storageNotificationManager.getProgressPercent(progress, size), notification.getProgressBar().getProgress());
    }

    private void checkDownloadCompleteNotification(ShadowNotification notification, BoxFile file) {
        String title = RuntimeEnvironment.application.
                getString(R.string.download_complete);
        String content = String.format(RuntimeEnvironment.application.
                getString(R.string.download_complete_msg), file.name);
        checkNotification(notification, title, content);
    }

    private void checkNotification(ShadowNotification notification, String title, String content) {
        assertEquals(title, notification.getContentTitle());
        assertEquals(content, notification.getContentText());
    }*/
}
