package de.qabel.qabelbox.storage.notifications;


import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

import org.apache.commons.io.FileUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.notifications.QblNotificationPresenter;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxTransferListener;
import de.qabel.qabelbox.storage.BoxUploadingFile;

public class StorageNotificationPresenter extends QblNotificationPresenter {

    private static final String TAG = StorageNotificationPresenter.class.getSimpleName();

    private final int UPLOAD_ID = generateId();
    private String lastUploadPath;
    private String lastOwner;
    private String lastFilename;

    private Map<String, Integer> downloadNotifications = new HashMap<>();

    public StorageNotificationPresenter(Context context) {
        super(context);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    public Intent createFileIntent(String identityId, String path) {
        Intent notificationIntent = new Intent(getContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.putExtra(MainActivity.ACTIVE_IDENTITY, identityId);
        notificationIntent.putExtra(MainActivity.START_FILES_FRAGMENT, true);
        //notificationIntent.putExtra(MainActivity.START_FILES_FRAGMENT_PATH, path);
        return notificationIntent;
    }

    public void updateUploadNotification(Queue<BoxUploadingFile> uploadingQueue) {
        BoxUploadingFile currentUpload = uploadingQueue.peek();
        if (currentUpload != null) {
            lastUploadPath = currentUpload.getPath();
            lastOwner = currentUpload.getOwnerIdentifier();
            lastFilename = currentUpload.name;

            Intent fileIntent = createFileIntent(lastOwner, lastUploadPath);
            int queueSize = uploadingQueue.size();
            String title = getContext().getResources().getQuantityString(R.plurals.uploadsNotificationTitle,
                    queueSize, (queueSize > 1 ? queueSize : currentUpload.name));
            String content = String.format(getString(R.string.upload_in_progress_notification_content), currentUpload.getUploadStatusPercent() + "%");

            showNotification(UPLOAD_ID, createNotification(
                    fileIntent, title, R.drawable.cloud_upload, content,
                    currentUpload.getUploadStatusPercent()));
        } else {
            Intent fileIntent = createFileIntent(lastOwner, lastUploadPath);
            showNotification(UPLOAD_ID, createNotification(
                    fileIntent, String.format(getString(R.string.upload_complete_notification_title)),
                    R.drawable.cloud_upload, String.format(getString(R.string.upload_complete_notification_msg), lastFilename), null));
        }
    }

    private void showNotification(int id, NotificationCompat.Builder builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_PROGRESS);
        }
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notify(id, notification);
    }

    public BoxTransferListener addDownloadNotifications(final String ownerKey, final String path, final BoxFile file) {
        int id = generateId();
        downloadNotifications.put(file.block, id);
        updateDownloadNotification(ownerKey, path, file, 0, file.size);
        return new BoxTransferListener() {
            @Override
            public void onProgressChanged(long bytesCurrent, long bytesTotal) {
                updateDownloadNotification(ownerKey, path, file, bytesCurrent, bytesTotal);
            }

            @Override
            public void onFinished() {
                updateDownloadNotification(ownerKey, path, file, 100, 100);
            }
        };
    }

    public void updateDownloadNotification(String ownerKey, String path, BoxFile file, long progress, long total) {
        Intent fileIntent = createFileIntent(ownerKey, path);
        Integer id = downloadNotifications.get(file.block);
        Integer progressValue = getProgressPercent(progress, total);
        String title, msg;
        if (progressValue < 100) {
            title = String.format(getString(R.string.downloading), file.name);
            msg = FileUtils.byteCountToDisplaySize(progress) + " / " + FileUtils.byteCountToDisplaySize(total);
        } else {
            progressValue = null;
            title = getString(R.string.download_complete);
            msg = String.format(getString(R.string.download_complete_msg), file.name);
        }
        showNotification(id, createNotification(fileIntent, title, R.drawable.download, msg, progressValue));
    }

    protected NotificationCompat.Builder createNotification(Intent intent, String contentTitle, int iconRes, String contentText, @Nullable Integer progress) {
        NotificationCompat.Builder builder = createNotification(intent, contentTitle, iconRes, contentText);
        if (progress != null) {
            builder.setProgress(100, progress, false);
        }
        return builder;
    }

    int getProgressPercent(long current, long total) {
        return (int) (100 * current / total);
    }

}
