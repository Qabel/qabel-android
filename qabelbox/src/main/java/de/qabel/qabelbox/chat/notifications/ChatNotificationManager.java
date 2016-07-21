package de.qabel.qabelbox.chat.notifications;

import java.util.List;

import de.qabel.qabelbox.chat.persistence.model.ChatMessageInfo;

public interface ChatNotificationManager {
    void updateNotifications(List<ChatMessageInfo> receivedMessages);
}
