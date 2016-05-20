package de.qabel.qabelbox.chat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.util.DefaultHashMap;

public class SyncAdapterChatNotificationManager implements ChatNotificationManager {

    private final NotificationManager notificationManager;
    private final Context context;
    private int currentId = 0;

    private Map<ChatMessageInfo, Integer> notified = new HashMap<>();

    public SyncAdapterChatNotificationManager(Context context) {
        this.context = context;
        this.notificationManager =(NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void updateNotifications(List<ChatMessageInfo> receivedMessages) {
        for (ChatMessageInfo msg: receivedMessages) {
            if (notified.containsKey(msg)) {
                continue;
            }
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra(MainActivity.ACTIVE_IDENTITY, msg.getIdentityKeyId());
            intent.putExtra(MainActivity.START_CONTACTS_FRAGMENT, true);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
            );
            NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
                    .setWhen(msg.getSent().getTime())
                    .setContentIntent(pendingIntent)
                    .setContentTitle(msg.getContactName())
                    .setContentText(msg.getMessage())
                    .setSmallIcon(R.drawable.qabel_logo);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                notification.setCategory(Notification.CATEGORY_MESSAGE);
            }
            notified.put(msg, currentId);
            notificationManager.notify(currentId++, notification.build());
        }
    }

}
