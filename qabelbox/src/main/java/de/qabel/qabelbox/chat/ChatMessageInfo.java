package de.qabel.qabelbox.chat;

import android.support.annotation.NonNull;

import java.util.Date;

public class ChatMessageInfo {

    @NonNull
    private Date sent;
    @NonNull
    private String contactName;
    @NonNull
    private String identityKeyId;
    @NonNull
    private String message;
    @NonNull
    private MessageType type;

    public ChatMessageInfo(@NonNull String contactName, @NonNull String identityKeyId,
                           @NonNull String message, @NonNull Date sent, @NonNull MessageType type) {
        this.sent = sent;
        this.contactName = contactName;
        this.identityKeyId = identityKeyId;
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
    public String getContactName() {
        return contactName;
    }

    public void setContactName(@NonNull String contactName) {
        this.contactName = contactName;
    }

    @NonNull
    public String getIdentityKeyId() {
        return identityKeyId;
    }

    public void setIdentityKeyId(@NonNull String identityKeyId) {
        this.identityKeyId = identityKeyId;
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
        if (!getContactName().equals(that.getContactName())) return false;
        if (!getIdentityKeyId().equals(that.getIdentityKeyId())) return false;
        if (!getMessage().equals(that.getMessage())) return false;
        return getType() == that.getType();

    }

    @Override
    public int hashCode() {
        int result = getSent().hashCode();
        result = 31 * result + getContactName().hashCode();
        result = 31 * result + getIdentityKeyId().hashCode();
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



    public enum MessageType {
        MESSAGE, SHARE
    }
}
