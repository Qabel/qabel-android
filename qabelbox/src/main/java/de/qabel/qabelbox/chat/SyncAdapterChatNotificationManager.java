package de.qabel.qabelbox.chat;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.util.DefaultHashMap;

public class SyncAdapterChatNotificationManager implements ChatNotificationManager {

    private final Context context;
    ChatNotificationPresenter chatNotificationPresenter;

    private Set<ChatMessageInfo> notified = new HashSet<>();

    @Inject
    public SyncAdapterChatNotificationManager(ChatNotificationPresenter chatNotificationPresenter,
                                              Context context) {
        this.chatNotificationPresenter = chatNotificationPresenter;
        this.context = context;
    }

    @Override
    public void updateNotifications(List<ChatMessageInfo> receivedMessages) {
        Iterable<ChatMessageInfo> messages = filterDuplicated(receivedMessages);
        for (ChatNotification msg: constructNotifications(messages)) {
            chatNotificationPresenter.showNotification(msg);
        }
    }

    public Iterable<ChatMessageInfo> filterDuplicated(List<ChatMessageInfo> receivedMessages) {
        List<ChatMessageInfo> messages = new ArrayList<>();
        for (ChatMessageInfo msg: receivedMessages) {
            if (notified.contains(msg)) {
                continue;
            }
            notified.add(msg);
            messages.add(msg);
        }
        return messages;
    }

    List<ChatNotification> constructNotifications(Iterable<ChatMessageInfo> messages) {
        DefaultHashMap<String, List<ChatMessageInfo>> map =
                new DefaultHashMap<>(contact -> new ArrayList<>());
        for (ChatMessageInfo msg: messages) {
            map.get(msg.getContactName()).add(msg);
        }
        List<ChatNotification> notifications = new ArrayList<>();
        for (List<ChatMessageInfo> contactMessages: map.values()) {
            ChatMessageInfo first = contactMessages.get(0);
            if (contactMessages.size() > 1) {
                String body = context.getString(R.string.new_messages, contactMessages.size());
                notifications.add(
                        new ChatNotification(first.getIdentityKeyId(), first.getContactName(),
                                body, first.getSent())
                );
            } else {
                notifications.add(
                        new ChatNotification(first.getIdentityKeyId(), first.getContactName(),
                                first.getMessage(), first.getSent())
                );
            }
        }
        return notifications;
    }

}
