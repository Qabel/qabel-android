package de.qabel.qabelbox.chat;

import java.util.List;

import de.qabel.core.drop.DropMessage;

public interface ChatNotificationManager {
    void updateNotifications(List<ChatMessageInfo> receivedMessages);
}
