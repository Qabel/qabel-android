package de.qabel.qabelbox.chat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.NotificationCompat;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import dagger.internal.Factory;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.util.DefaultHashMap;

public class AndroidChatNotificationPresenter implements ChatNotificationPresenter {

    private Provider<NotificationCompat.Builder> builder;
    Context context;
    NotificationManager notificationManager;
    private int currentId = 0;

    Map<String, Integer> identityToNotificationId = new DefaultHashMap<>(
            identity -> currentId++
    );

    @Inject
    public AndroidChatNotificationPresenter(Context context,
                                            Factory<NotificationCompat.Builder> builder) {
        this.notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        this.context = context;
        this.builder = builder;
    }

    @Override
    public void showNotification(ChatNotification notification) {
        Intent intent = getIntent(notification);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        NotificationCompat.Builder notificationBuilder = builder.get();
        notificationBuilder.setDefaults(Notification.DEFAULT_ALL)
               .setWhen(notification.when.getTime())
               .setContentIntent(pendingIntent)
               .setContentTitle(notification.contactHeader)
               .setContentText(notification.message)
               .setSmallIcon(R.drawable.qabel_logo)
               .setPriority(Notification.PRIORITY_HIGH)
               .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setCategory(Notification.CATEGORY_MESSAGE);
        }
        notificationManager.notify("ChatNotification", identityToNotificationId.get(
                notification.identity.getKeyIdentifier()),
                notificationBuilder.build());
    }

    @NonNull
    Intent getIntent(ChatNotification notification) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.ACTIVE_IDENTITY, notification.identity.getKeyIdentifier());
        intent.putExtra(MainActivity.START_CONTACTS_FRAGMENT, true);
        if (notification.contact != null) {
            intent.putExtra(MainActivity.ACTIVE_CONTACT, notification.contact.getKeyIdentifier());
        }
        return intent;
    }
}
