package de.qabel.qabelbox.services;

import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.Map;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropPayloadSizeException;

public interface DropConnector {
    void sendDropMessage(DropMessage dropMessage, Contact recipient,
                         Identity identity,
                         @Nullable LocalQabelService.OnSendDropMessageResult dropResultCallback)
            throws QblDropPayloadSizeException;

    Collection<DropMessage> retrieveDropMessages(Identity identity, long sinceDate);

    interface OnSendDropMessageResult {
        void onSendDropResult(Map<DropURL, Boolean> deliveryStatus);
    }
}
