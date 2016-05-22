package de.qabel.qabelbox.storage.notifications;


import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.NotificationCompat;

import java.util.Queue;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.notifications.QblNotificationManager;
import de.qabel.qabelbox.storage.BoxUploadingFile;

public class StorageNotificationManager extends QblNotificationManager {

    private static final String TAG = StorageNotificationManager.class.getSimpleName();

    private final int UPLOAD_ID = generateId();
    private String lastUploadPath;
    private String lastOwner;

    public StorageNotificationManager(Context context) {
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
            Intent fileIntent = createFileIntent(lastOwner, lastUploadPath);
            String title = getContext().getResources().getQuantityString(R.plurals.uploadsNotificationTitle,
                    uploadingQueue.size(), uploadingQueue.size());
            String content = String.format(getContext().getString(R.string.upload_in_progress_notification_content), currentUpload.name);

            showNotification(createNotification(
                    fileIntent, title, R.drawable.cloud_upload, content,
                    currentUpload.getUploadStatusPercent()));
        } else {
            Intent fileIntent = createFileIntent(lastOwner, lastUploadPath);
            showNotification(createNotification(
                    fileIntent, getContext().getResources().
                            getString(R.string.upload_complete_notification_title),
                    R.drawable.cloud_upload, null, null));
        }
    }

    private void showNotification(NotificationCompat.Builder builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_PROGRESS);
        }
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notify(UPLOAD_ID, notification);
    }

}
