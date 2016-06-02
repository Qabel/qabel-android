package de.qabel.qabelbox.storage.notifications;

import android.app.NotificationManager;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.verification.VerificationMode;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.storage.model.BoxFile;
import de.qabel.qabelbox.storage.model.BoxUploadingFile;
import de.qabel.qabelbox.storage.transfer.BoxTransferListener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class StorageNotificationManagerTest {

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
        StorageNotificationInfo expectedInfo = new StorageNotificationInfo(TEST_FILE_NAME, TEST_UPLOAD_PATH, TEST_OWNER, 0, 1);
        storageNotificationManager.updateUploadNotification(queue.size(), uploadingFile);
        verify(fakePresenter).updateUploadNotification(1, expectedInfo);

        queue.poll();
        expectedInfo.setProgress(uploadingFile.uploadedSize, uploadingFile.totalSize);
        storageNotificationManager.updateUploadNotification(queue.size(), queue.peek());
        verify(fakePresenter).updateUploadNotification(0, expectedInfo);
    }

    @Test
    public void testMultipleUpload() {

        long size = 200L;

        Queue<BoxUploadingFile> queue = new LinkedList<>();
        queue.add(new BoxUploadingFile("FILE", TEST_UPLOAD_PATH, TEST_OWNER));
        queue.add(new BoxUploadingFile("FILE2", TEST_UPLOAD_PATH, TEST_OWNER));
        queue.add(new BoxUploadingFile("FILE3", TEST_UPLOAD_PATH, TEST_OWNER));

        StorageNotificationInfo expectedInfo = new StorageNotificationInfo("FILE",
                TEST_UPLOAD_PATH, TEST_OWNER, 0L, 1L);
        //3 Uploads
        storageNotificationManager.updateUploadNotification(queue.size(), queue.peek());
        verify(fakePresenter).updateUploadNotification(3, expectedInfo);

        //2 Uploads
        queue.poll();
        long progress = 100L;
        BoxUploadingFile current = queue.peek();
        current.totalSize = size;
        current.uploadedSize = progress;
        expectedInfo = new StorageNotificationInfo("FILE2", TEST_UPLOAD_PATH, TEST_OWNER,
                progress, size);
        storageNotificationManager.updateUploadNotification(queue.size(), queue.peek());
        verify(fakePresenter).updateUploadNotification(2, expectedInfo);

        //1 Upload
        queue.poll();
        expectedInfo = new StorageNotificationInfo("FILE3", TEST_UPLOAD_PATH, TEST_OWNER, 0L, 1L);
        storageNotificationManager.updateUploadNotification(queue.size(), queue.peek());
        verify(fakePresenter).updateUploadNotification(1, expectedInfo);

        //Uploads complete
        BoxUploadingFile lastFile = queue.poll();
        lastFile.uploadedSize = size;
        lastFile.totalSize = size;
        expectedInfo = new StorageNotificationInfo("FILE3", TEST_UPLOAD_PATH, TEST_OWNER, size, size);
        storageNotificationManager.updateUploadNotification(queue.size(), queue.peek());
        verify(fakePresenter).updateUploadNotification(0, expectedInfo);
    }

    @Test
    public void testSingleDownload() {
        String ownerKey = "thisIsMyFile";
        String path = "/";
        long size = 4048L;
        BoxFile file = new BoxFile(TestConstants.PREFIX, "block", "testfile", size,
                System.currentTimeMillis(), new byte[]{0x01, 0x02});
        StorageNotificationInfo expectedInfo = new StorageNotificationInfo("testfile", path, ownerKey, 0L, size);
        BoxTransferListener listener = storageNotificationManager.
                addDownloadNotification(ownerKey, path, file);
        listener.onProgressChanged(0L, size);
        verify(fakePresenter).updateDownloadNotification(expectedInfo);

        //Check Progress
        long progress = size / 2;
        listener.onProgressChanged(progress, size);
        expectedInfo.setProgress(progress, size);
        verify(fakePresenter, VerificationModeFactory.atLeastOnce()).updateDownloadNotification(expectedInfo);

        progress += file.size / 3;
        listener.onProgressChanged(progress, size);
        expectedInfo.setProgress(progress, size);
        verify(fakePresenter, VerificationModeFactory.atLeastOnce()).updateDownloadNotification(expectedInfo);


        listener.onFinished();
        expectedInfo.complete();
        verify(fakePresenter, VerificationModeFactory.atLeastOnce()).updateDownloadNotification(expectedInfo);
    }

    @Test
    public void testMultiDownload() {
        String ownerKey = "thisIsMyFile";
        String path = "/";
        long sizeA = 4048L;
        long sizeB = 4048L * 2;
        Map<BoxFile, BoxTransferListener> fileMap = new HashMap<>();

        BoxFile fileA = new BoxFile(TestConstants.PREFIX, "block", "testfile",
                sizeA, System.currentTimeMillis(), new byte[]{0x01, 0x02});
        StorageNotificationInfo expectedInfoA = new StorageNotificationInfo(
                "testfile", path, ownerKey, 0L, sizeA);

        BoxTransferListener listenerA = storageNotificationManager.addDownloadNotification(ownerKey, path, fileA);
        listenerA.onProgressChanged(0L, sizeA);
        verify(fakePresenter).updateDownloadNotification(expectedInfoA);

        StorageNotificationInfo expectedInfoB = new StorageNotificationInfo(
                "testfile2", path, ownerKey, 0L, sizeB);
        BoxFile fileB = new BoxFile(TestConstants.PREFIX, "block2", "testfile2", sizeB, System.currentTimeMillis(), new byte[]{0x01, 0x02});
        BoxTransferListener listenerB = storageNotificationManager.addDownloadNotification(ownerKey, path, fileB);
        listenerB.onProgressChanged(0L, sizeB);
        verify(fakePresenter).updateDownloadNotification(expectedInfoB);

        expectedInfoA.setProgress(50, sizeA);
        listenerA.onProgressChanged(50, fileA.size);
        verify(fakePresenter, VerificationModeFactory.atLeastOnce()).updateDownloadNotification(expectedInfoB);

        expectedInfoB.setProgress(100, sizeB);
        listenerB.onProgressChanged(100, fileB.size);
        verify(fakePresenter, VerificationModeFactory.atLeastOnce()).updateDownloadNotification(expectedInfoB);

        expectedInfoB.complete();
        listenerB.onFinished();
        verify(fakePresenter, VerificationModeFactory.atLeastOnce()).updateDownloadNotification(expectedInfoB);

        expectedInfoA.complete();
        listenerA.onFinished();
        verify(fakePresenter, VerificationModeFactory.atLeastOnce()).updateDownloadNotification(expectedInfoA);
    }
}
