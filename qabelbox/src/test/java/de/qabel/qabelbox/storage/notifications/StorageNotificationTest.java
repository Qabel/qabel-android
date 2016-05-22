package de.qabel.qabelbox.storage.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowNotification;
import org.robolectric.shadows.ShadowNotificationManager;

import java.util.LinkedList;
import java.util.Queue;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.storage.BoxUploadingFile;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class StorageNotificationTest {

    private static final String TEST_OWNER = "owner";
    private static final String UPLOAD_PATH = "path";

    private NotificationManager notificationManager;
    private StorageNotificationManager storageNotificationManager;

    @Before
    public void setUp() {
        notificationManager = (NotificationManager) RuntimeEnvironment.application
                .getSystemService(Context.NOTIFICATION_SERVICE);
        shadowOf(notificationManager).cancelAll();
        storageNotificationManager = new StorageNotificationManager(
                RuntimeEnvironment.application.getApplicationContext());
    }

    @Test
    public void testSingleUpload() {
        BoxUploadingFile uploadingFile = new BoxUploadingFile("FILE", UPLOAD_PATH, TEST_OWNER);
        Queue<BoxUploadingFile> queue = new LinkedList<>();
        queue.add(uploadingFile);
        storageNotificationManager.updateUploadNotification(queue);
        checkNotification(queue);

        queue.poll();
        storageNotificationManager.updateUploadNotification(queue);
        checkNotification(queue);
    }

    @Test
    public void testMultipleUpload() {
        Queue<BoxUploadingFile> queue = new LinkedList<>();
        queue.add(new BoxUploadingFile("FILE", UPLOAD_PATH, TEST_OWNER));
        queue.add(new BoxUploadingFile("FILE2", UPLOAD_PATH, TEST_OWNER));
        queue.add(new BoxUploadingFile("FILE3", UPLOAD_PATH, TEST_OWNER));
        //3 Uploads
        storageNotificationManager.updateUploadNotification(queue);
        checkNotification(queue);

        //2 Uploads
        queue.poll();
        storageNotificationManager.updateUploadNotification(queue);
        checkNotification(queue);
        //1 Upload

        queue.poll();
        storageNotificationManager.updateUploadNotification(queue);
        checkNotification(queue);

        //Uploads complete
        queue.poll();
        storageNotificationManager.updateUploadNotification(queue);
        checkNotification(queue);
    }

    private void checkNotification(Queue<BoxUploadingFile> queue) {
        BoxUploadingFile currentUpload = queue.peek();

        ShadowNotificationManager shadowManger = shadowOf(notificationManager);
        assertEquals(1, shadowManger.size());
        Notification notification = shadowManger.getAllNotifications().get(0);
        ShadowNotification shadowNotification = shadowOf(notification);

        int expectedProgress = (currentUpload == null ? 100 : currentUpload.getUploadStatusPercent());
        assertEquals(expectedProgress, shadowNotification.getProgressBar().getProgress());

        String expectedTitle = (queue.size() > 0 ?
                RuntimeEnvironment.application.getResources().
                        getQuantityString(R.plurals.uploadsNotificationTitle, queue.size(), queue.size())
                : RuntimeEnvironment.application.getString(R.string.upload_complete_notification_title));
        assertEquals(expectedTitle, shadowNotification.getContentTitle());

        String expectedContent = null;
        if (currentUpload != null) {
            expectedContent = String.format(RuntimeEnvironment.application.getString(R.string.upload_in_progress_notification_content), currentUpload.name);
        }
        assertEquals(expectedContent, shadowNotification.getContentText());
    }
}
