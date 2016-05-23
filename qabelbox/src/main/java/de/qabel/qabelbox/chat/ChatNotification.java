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
}
