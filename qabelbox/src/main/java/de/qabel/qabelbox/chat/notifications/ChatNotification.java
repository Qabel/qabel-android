package de.qabel.qabelbox.chat.notifications;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Date;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;

public class ChatNotification {
    public Identity identity;
    public String contactHeader;
    public String message;
    public Date when;
    @Nullable
    public Contact contact;

    public ChatNotification(@NonNull Identity identity,
                            @NonNull String contactHeader,
                            @NonNull String message,
                            @NonNull Date when) {
        this.identity = identity;
        this.contactHeader = contactHeader;
        this.message = message;
        this.when = when;
    }

    public ChatNotification(@NonNull Identity identity,
                            @NonNull Contact contact,
                            @NonNull String message,
                            @NonNull Date when) {
        this.identity = identity;
        this.contactHeader = contact.getAlias();
        this.message = message;
        this.when = when;
        this.contact = contact;
    }

    @Override
    public String toString() {
        return "ChatNotification{" +
                "identity=" + identity +
                ", contactHeader='" + contactHeader + '\'' +
                ", message='" + message + '\'' +
                ", when=" + when +
                ", contact=" + contact +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatNotification)) return false;

        ChatNotification that = (ChatNotification) o;

        if (!identity.equals(that.identity)) return false;
        if (!contactHeader.equals(that.contactHeader)) return false;
        if (!message.equals(that.message)) return false;
        if (!when.equals(that.when)) return false;
        return contact != null ? contact.equals(that.contact) : that.contact == null;

    }

    @Override
    public int hashCode() {
        int result = identity.hashCode();
        result = 31 * result + contactHeader.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + when.hashCode();
        result = 31 * result + (contact != null ? contact.hashCode() : 0);
        return result;
    }
}
