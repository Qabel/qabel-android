package de.qabel.qabelbox.chat;

import android.support.annotation.NonNull;

import java.util.Date;

public class ChatNotification {
    public String identityId;
    public String contactHeader;
    public String message;
    public Date when;

    public ChatNotification(@NonNull String identityId,
                            @NonNull String contactHeader,
                            @NonNull String message,
                            @NonNull Date when) {
        this.identityId = identityId;
        this.contactHeader = contactHeader;
        this.message = message;
        this.when = when;
    }

    @Override
    public String toString() {
        return "ChatNotification{" +
                "identityId='" + identityId + '\'' +
                ", contactHeader='" + contactHeader + '\'' +
                ", message='" + message + '\'' +
                ", when=" + when +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatNotification)) return false;

        ChatNotification that = (ChatNotification) o;

        if (!identityId.equals(that.identityId)) return false;
        if (!contactHeader.equals(that.contactHeader)) return false;
        if (!message.equals(that.message)) return false;
        return when.equals(that.when);

    }

    @Override
    public int hashCode() {
        int result = identityId.hashCode();
        result = 31 * result + contactHeader.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + when.hashCode();
        return result;
    }
}
