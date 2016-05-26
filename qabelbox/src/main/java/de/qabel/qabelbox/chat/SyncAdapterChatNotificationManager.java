package de.qabel.qabelbox.chat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class SyncAdapterChatNotificationManager implements ChatNotificationManager {

    ChatNotificationPresenter chatNotificationPresenter;

    private Set<ChatMessageInfo> notified = new HashSet<>();

    @Inject
    public SyncAdapterChatNotificationManager(ChatNotificationPresenter chatNotificationPresenter) {
        this.chatNotificationPresenter = chatNotificationPresenter;
    }

    @Override
    public void updateNotifications(List<ChatMessageInfo> receivedMessages) {
        for (ChatMessageInfo msg: receivedMessages) {
            if (notified.contains(msg)) {
                continue;
            }
            notified.add(msg);
            chatNotificationPresenter.showNotification(new ChatNotification(
                    msg.getIdentityKeyId(), msg.getContactName(), msg.getMessage(), msg.getSent()));
        }
    }

}
