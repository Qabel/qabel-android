package de.qabel.qabelbox.chat;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.util.DefaultHashMap;

import static de.qabel.qabelbox.chat.ChatNotification.Target.CHAT;
import static de.qabel.qabelbox.chat.ChatNotification.Target.LIST;

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
        for (List<ChatMessageInfo> byIdentity: filterDuplicated(receivedMessages).values()) {
            for (ChatNotification msg: constructNotifications(byIdentity)) {
                chatNotificationPresenter.showNotification(msg);
            }
        }
    }

    public DefaultHashMap<String, List<ChatMessageInfo>> filterDuplicated(List<ChatMessageInfo> receivedMessages) {
        DefaultHashMap<String, List<ChatMessageInfo>> messages = new DefaultHashMap<>(identity -> new ArrayList<>());
        for (ChatMessageInfo msg: receivedMessages) {
            if (notified.contains(msg)) {
                continue;
            }
            notified.add(msg);
            messages.get(msg.getIdentity().getKeyIdentifier()).add(msg);
        }
        return messages;
    }

    List<ChatNotification> constructNotifications(Iterable<ChatMessageInfo> messages) {
        DefaultHashMap<String, List<ChatMessageInfo>> byContact =
                new DefaultHashMap<>(contact -> new ArrayList<>());
        for (ChatMessageInfo msg: messages) {
            byContact.get(msg.getContact().getKeyIdentifier()).add(msg);
        }
        List<ChatNotification> notifications = new ArrayList<>();
        for (List<ChatMessageInfo> contactMessages: byContact.values()) {
            ChatMessageInfo first = contactMessages.get(0);
            if (contactMessages.size() > 1) {
                String body = context.getString(R.string.new_messages, contactMessages.size());
                notifications.add(
                        new ChatNotification(first.getIdentity(), first.getContact(),
                                body, first.getSent())
                );
            } else {
                notifications.add(
                        new ChatNotification(first.getIdentity(), first.getContact(),
                                first.getMessage(), first.getSent())
                );
            }
        }
        return notifications;
    }

}
