package de.qabel.qabelbox.services;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropPayloadSizeException;
import de.qabel.qabelbox.util.DefaultHashMap;

public class MockedDropConnector implements DropConnector {

    public Map<String, List<DropMessage>> messages = new DefaultHashMap<>(
            new DefaultHashMap.DefaultValueFactory<String, List<DropMessage>>() {
                @Override
                public List<DropMessage> defaultValueFor(String identity) {
                    return new ArrayList<>();
                }
            });

    @Override
    public void sendDropMessage(DropMessage dropMessage, Contact recipient, Identity identity,
                                @Nullable OnSendDropMessageResult dropResultCallback)
            throws QblDropPayloadSizeException {
        messages.get(identity.getKeyIdentifier()).add(dropMessage);
        if (dropResultCallback != null) {
            dropResultCallback.onSendDropResult(new HashMap<DropURL, Boolean>());
        }

    }

    @Override
    public Collection<DropMessage> retrieveDropMessages(Identity identity, long sinceDate) {
        ArrayList<DropMessage> filtered = new ArrayList<>();
        for (DropMessage m: messages.get(identity.getKeyIdentifier())) {
            if (m.getCreationDate().after(new Date(sinceDate))) {
                filtered.add(m);
            }
        }
        return filtered;
    }


}
