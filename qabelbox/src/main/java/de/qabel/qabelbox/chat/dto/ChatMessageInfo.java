package de.qabel.qabelbox.chat.dto;

import android.support.annotation.NonNull;

import java.util.Date;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;

public class ChatMessageInfo implements Comparable<ChatMessageInfo> {

    @NonNull
    private Date sent;
    @NonNull
    private Contact contact;
    @NonNull
    private Identity identity;
    @NonNull
    private String message;
    @NonNull
    private MessageType type;

    public ChatMessageInfo(@NonNull Contact contact, @NonNull Identity identity,
                           @NonNull String message, @NonNull Date sent, @NonNull MessageType type) {
        this.sent = sent;
        this.contact = contact;
        this.identity = identity;
        this.message = message;
        this.type = type;
    }

    @NonNull
    public Date getSent() {
        return sent;
    }

    public void setSent(@NonNull Date sent) {
        this.sent = sent;
    }

    @NonNull
    public Contact getContact() {
        return contact;
    }

    public void setContact(@NonNull Contact contact) {
        this.contact = contact;
    }

    @NonNull
    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(@NonNull Identity identity) {
        this.identity = identity;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    public void setMessage(@NonNull String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatMessageInfo)) return false;

        ChatMessageInfo that = (ChatMessageInfo) o;

        if (!getSent().equals(that.getSent())) return false;
        if (!getContact().equals(that.getContact())) return false;
        if (!getIdentity().equals(that.getIdentity())) return false;
        if (!getMessage().equals(that.getMessage())) return false;
        return getType() == that.getType();

    }

    @Override
    public int hashCode() {
        int result = getSent().hashCode();
        result = 31 * result + getContact().hashCode();
        result = 31 * result + getIdentity().hashCode();
        result = 31 * result + getMessage().hashCode();
        result = 31 * result + getType().hashCode();
        return result;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    @Override
    public int compareTo(ChatMessageInfo another) {
        if (another == null) { return 1; }
        return sent.compareTo(another.getSent());
    }


    public enum MessageType {
        MESSAGE, SHARE
    }
}
