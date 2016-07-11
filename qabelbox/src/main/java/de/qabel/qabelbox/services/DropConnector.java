package de.qabel.qabelbox.services;

import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropPayloadSizeException;
import kotlin.Pair;

public interface DropConnector {
    void sendDropMessage(DropMessage dropMessage, Contact recipient,
                         Identity identity,
                         @Nullable OnSendDropMessageResult dropResultCallback)
            throws QblDropPayloadSizeException;

    RetrieveDropMessagesResult retrieveDropMessages(Identity identity, long sinceDate);

    interface OnSendDropMessageResult {
        void onSendDropResult(Map<DropURL, Boolean> deliveryStatus);
    }

    class RetrieveDropMessagesResult {
        private Collection<DropMessage> messages;
        private long sinceDate;

        public RetrieveDropMessagesResult(Collection<DropMessage> messages, long sinceDate) {
            this.messages = messages;
            this.sinceDate = sinceDate;
        }

        public Collection<DropMessage> getMessages() {
            return messages;
        }

        public long getSinceDate() {
            return sinceDate;
        }
    }
}
