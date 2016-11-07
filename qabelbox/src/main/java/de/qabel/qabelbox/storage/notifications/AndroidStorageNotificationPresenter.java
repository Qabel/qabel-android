package de.qabel.qabelbox.storage.notifications;


import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import org.apache.commons.io.FileUtils;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.base.ActiveIdentityActivity;
import de.qabel.qabelbox.base.MainActivity;
import de.qabel.qabelbox.notifications.QblNotificationPresenter;

public class AndroidStorageNotificationPresenter extends QblNotificationPresenter
        implements StorageNotificationPresenter {

    private static final String TAG = AndroidStorageNotificationPresenter.class.getSimpleName();

    public AndroidStorageNotificationPresenter(Context context) {
        super(context);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    public Intent createFileIntent(StorageNotificationInfo info) {
        Intent notificationIntent = new Intent(getContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationIntent.putExtra(ActiveIdentityActivity.Constants.ACTIVE_IDENTITY, info.getIdentityKeyId());
        notificationIntent.putExtra(MainActivity.START_FILES_FRAGMENT, true);
        notificationIntent.putExtra(MainActivity.START_FILES_FRAGMENT_PATH, info.getPath());
        return notificationIntent;
    }

    @Override
    public void updateUploadNotification(int queueSize, StorageNotificationInfo info) {
        int notificationId = getIdForInfo(info);
        Intent fileIntent = createFileIntent(info);
        NotificationCompat.Builder notificationBuilder;
        if (!info.isComplete()) {
            String title = getContext().getResources().
                    getQuantityString(R.plurals.uploadsNotificationTitle,
                            queueSize, (queueSize > 1 ? queueSize : info.getFileName()));
            String content = String.format(
                    getString(R.string.upload_in_progress_notification_content),
                    info.getProgress() + "%");
            notificationBuilder = createNotification(
                    fileIntent, title, R.drawable.cloud_upload, content);
            notificationBuilder.setProgress(100, info.getProgress(), false);
        } else {
            notificationBuilder = createNotification(
                    fileIntent,
                    String.format(getString(R.string.upload_complete_notification_title)),
                    R.drawable.cloud_upload,
                    String.format(getString(R.string.upload_complete_notification_msg),
                            info.getFileName()));
        }
        showNotification(notificationId, notificationBuilder);
    }

    private void showNotification(int id, NotificationCompat.Builder builder) {
        notify(id, builder, Notification.CATEGORY_PROGRESS, false);
    }

    @Override
    public void updateDownloadNotification(StorageNotificationInfo info) {
        Intent fileIntent = createFileIntent(info);
        String title, msg;
        if (!info.isComplete()) {
            title = String.format(getString(R.string.downloading), info.getFileName());
            msg = FileUtils.byteCountToDisplaySize(info.getDoneBytes())
                    + " / " + FileUtils.byteCountToDisplaySize(info.getTotalBytes());
        } else {
            title = getString(R.string.download_complete);
            msg = String.format(getString(R.string.download_complete_msg), info.getFileName());
        }
        NotificationCompat.Builder notificationBuilder =
                createNotification(fileIntent, title, R.drawable.download, msg);

        if (!info.isComplete()) {
            notificationBuilder.setProgress(100, info.getProgress(), false);
        }
        showNotification(getIdForInfo(info), notificationBuilder);
    }
}
