package de.qabel.qabelbox.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

import de.qabel.qabelbox.util.DefaultHashMap;

public abstract class QblNotificationPresenter<T, V extends QblNotificationInfo> {

    private final NotificationManager notificationManager;
    private final Context context;

    private int idSequence = 0;

    private DefaultHashMap<T, Integer> infoMap = new DefaultHashMap<>(
            new DefaultHashMap.DefaultValueFactory<T, Integer>() {
                @Override
                public Integer defaultValueFor(T key) {
                    return idSequence++;
                }
            });

    public QblNotificationPresenter(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    protected abstract String getTag();

    protected int getIdForInfo(V info) {
        return infoMap.get(info.getIdentifier());
    }

    protected Context getContext() {
        return this.context;
    }

    protected PendingIntent getPendingIntent(Intent intent) {
        return PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    protected void notify(int id, NotificationCompat.Builder builder, String category, boolean autoCancel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(category);
        }
        Notification notification = builder.build();
        if(!autoCancel){
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
        }
        notificationManager.notify(getTag(), id, notification);
    }

    protected NotificationCompat.Builder createNotification(Intent intent, String contentTitle, int iconRes, @Nullable String contentText) {
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSmallIcon(iconRes)
                .setContentIntent(getPendingIntent(intent));

        return builder;
    }

    protected String getString(int resId) {
        return getContext().getString(resId);
    }
    protected String getString(int resId, Object... args) {
        return getContext().getString(resId, args);
    }


}
