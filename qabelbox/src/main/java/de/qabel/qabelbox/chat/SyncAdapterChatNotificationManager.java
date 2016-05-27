package de.qabel.qabelbox.chat;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import de.qabel.core.config.Contact;
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
        for (List<ChatMessageInfo> byIdentity: filterDuplicated(receivedMessages).values()) {
            ChatNotification chatNotification = constructNotification(byIdentity);
            if (chatNotification != null) {
                chatNotificationPresenter.showNotification(chatNotification);
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

    @Nullable
    ChatNotification constructNotification(List<ChatMessageInfo> messages) {
        if (messages.size() == 0) {
            return null;
        }
        Collections.sort(messages);
        ChatMessageInfo first = messages.get(0);
        DefaultHashMap<Contact, Integer> byContact = countMessagesByContact(messages);
        if (byContact.size() > 1) {
            String header = createContactsHeader(byContact);
            String body = context.getString(R.string.new_messages, messages.size());
            return new ChatNotification(first.getIdentity(), header, body, first.getSent());
        } else {
            if (messages.size() > 1) {
                String body = context.getString(R.string.new_messages, messages.size());
                return new ChatNotification(first.getIdentity(), first.getContact(),
                                body, first.getSent());
            } else {
                return new ChatNotification(first.getIdentity(), first.getContact(),
                                first.getMessage(), first.getSent());
            }
        }
    }

    @NonNull
    private DefaultHashMap<Contact, Integer> countMessagesByContact(List<ChatMessageInfo> messages) {
        DefaultHashMap<Contact, Integer> byContact = new DefaultHashMap<>(contact -> 0);
        for (ChatMessageInfo msg: messages) {
            byContact.put(msg.getContact(), byContact.get(msg.getContact()) + 1);
        }
        return byContact;
    }

    private String createContactsHeader(DefaultHashMap<Contact, Integer> byContact) {
        List<String> names = new ArrayList<>();
        for (Contact contact: byContact.keySet()) {
            names.add(contact.getAlias());
        }
        Collections.sort(names);
        return StringUtils.join(names, ", ");
    }

}
