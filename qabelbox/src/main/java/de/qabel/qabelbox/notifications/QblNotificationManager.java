package de.qabel.qabelbox.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

public abstract class QblNotificationManager {

    private final NotificationManager notificationManager;
    private final Context context;

    private int idSequence = 0;

    public QblNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    protected abstract String getTag();

    protected int generateId() {
        return idSequence++;
    }

    protected Context getContext() {
        return this.context;
    }

    protected void notify(int id, Notification notification) {
        notificationManager.notify(getTag(), id, notification);
    }

    protected NotificationCompat.Builder createNotification(Intent intent, String contentTitle, int iconRes, @Nullable String contentText) {
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSmallIcon(iconRes)
                .setContentIntent(pendingIntent);

        return builder;
    }

    protected String getString(int resId) {
        return getContext().getString(resId);
    }


}
