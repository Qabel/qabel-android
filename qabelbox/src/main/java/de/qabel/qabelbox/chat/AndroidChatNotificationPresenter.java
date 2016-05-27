package de.qabel.qabelbox.chat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import java.util.Map;

import javax.inject.Inject;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.util.DefaultHashMap;

public class AndroidChatNotificationPresenter implements ChatNotificationPresenter {

    Context context;
    NotificationManager notificationManager;
    private int currentId = 0;

    Map<String, Integer> identityToNotificationId = new DefaultHashMap<>(
            identity -> currentId++
    );

    @Inject
    public AndroidChatNotificationPresenter(Context context) {
        this.notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        this.context = context;
    }

    @Override
    public void showNotification(ChatNotification notification) {
        Intent intent = getIntent(notification);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        NotificationCompat.Builder androidNotification = new NotificationCompat.Builder(context)
                .setWhen(notification.when.getTime())
                .setContentIntent(pendingIntent)
                .setContentTitle(notification.contactHeader)
                .setContentText(notification.message)
                .setSmallIcon(R.drawable.qabel_logo)
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            androidNotification.setCategory(Notification.CATEGORY_MESSAGE);
        }
        notificationManager.notify("ChatNotification", identityToNotificationId.get(
                notification.identity.getKeyIdentifier()),
                androidNotification.build());
    }

    @NonNull
    Intent getIntent(ChatNotification notification) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                       | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.ACTIVE_IDENTITY, notification.identity.getKeyIdentifier());
        intent.putExtra(MainActivity.START_CONTACTS_FRAGMENT, true);
        if (notification.contact != null) {
            intent.putExtra(MainActivity.ACTIVE_CONTACT, notification.contact.getKeyIdentifier());
        }
        return intent;
    }
}
