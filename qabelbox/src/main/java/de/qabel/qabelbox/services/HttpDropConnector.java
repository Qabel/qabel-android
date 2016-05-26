package de.qabel.qabelbox.services;

import android.support.annotation.Nullable;
import android.util.Log;

import java.net.URI;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.AbstractBinaryDropMessage;
import de.qabel.core.crypto.BinaryDropMessageV0;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropInvalidMessageSizeException;
import de.qabel.core.exceptions.QblDropPayloadSizeException;
import de.qabel.core.exceptions.QblSpoofedSenderException;
import de.qabel.core.exceptions.QblVersionMismatchException;
import de.qabel.core.http.DropHTTP;
import de.qabel.core.http.HTTPResult;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.exception.PersistenceException;

public class HttpDropConnector implements DropConnector {
    private static final String TAG = "HttpDropConnector";
    private DropHTTP dropHTTP;
    private IdentityRepository identityRepository;
    private ContactRepository contactRepository;

    @Inject
    public HttpDropConnector(IdentityRepository identityRepository, ContactRepository contactRepository) {
        this.identityRepository = identityRepository;
        this.contactRepository = contactRepository;
        this.dropHTTP = new DropHTTP();
    }

    /**
     * Sends {@link DropMessage} to a {@link Contact} in a new thread. Returns without blocking.
     *
     * @param dropMessage        {@link DropMessage} to send.
     * @param recipient          {@link Contact} to send {@link DropMessage} to.
     * @param dropResultCallback Callback to Map<DropURL, Boolean> deliveryStatus which contains
     *                           sending status to DropURLs of the recipient. Can be null if status is irrelevant.
     * @throws QblDropPayloadSizeException
     */
    @Override
    public void sendDropMessage(final DropMessage dropMessage, final Contact recipient,
                                final Identity identity,
                                @Nullable final LocalQabelService.OnSendDropMessageResult dropResultCallback)
            throws QblDropPayloadSizeException {
        new Thread(new Runnable() {
            final BinaryDropMessageV0 binaryMessage = new BinaryDropMessageV0(dropMessage);
            final byte[] messageByteArray = binaryMessage.assembleMessageFor(recipient, identity);
            HashMap<DropURL, Boolean> deliveryStatus = new HashMap<>();

            @Override
            public void run() {
                if (recipient.getDropUrls().size() == 0) {
                    Log.e(TAG, "no dropurls in recipient");
                }
                for (DropURL dropURL : recipient.getDropUrls()) {
                    HTTPResult<?> dropResult = dropHTTPsend(dropURL, messageByteArray);
                    if (dropResult.getResponseCode() == 200) {
                        deliveryStatus.put(dropURL, true);
                    } else {
                        deliveryStatus.put(dropURL, false);
                    }
                }
                if (dropResultCallback != null) {
                    dropResultCallback.onSendDropResult(deliveryStatus);
                }
            }
        }).start();
    }

    /**
     * Send DropMessages via DropHTTP. Method extracted to mock send in LocalQabelServiceTester.
     *
     * @param dropURL DropURL to send DropMessage to.
     * @param message Encrypted DropMessage
     * @return
     */
    HTTPResult<?> dropHTTPsend(DropURL dropURL, byte[] message) {
        return dropHTTP.send(dropURL.getUri(), message);
    }


    /**
     * Retrieves all DropMessages for given Identities
     *
     * @return Retrieved, decrypted DropMessages.
     */
    @Override
    public Collection<DropMessage> retrieveDropMessages(Identity identity, long sinceDate) {
        Collection<DropMessage> allMessages = new ArrayList<>();

        for (DropURL dropUrl : identity.getDropUrls()) {
            Collection<DropMessage> results = this.retrieveDropMessages(dropUrl.getUri(), sinceDate);
            allMessages.addAll(results);

        }
        return allMessages;
    }

    /**
     * Retrieves all DropMessages from given URI
     *
     * @param uri URI where to retrieve the drop from
     * @return Retrieved, decrypted DropMessages.
     */
    public Collection<DropMessage> retrieveDropMessages(URI uri, long sinceDate) {
        HTTPResult<Collection<byte[]>> cipherMessages = getDropMessages(uri, sinceDate);
        Collection<DropMessage> plainMessages = new ArrayList<>();

        List<Contact> ccc = new ArrayList<>();
        for (Contacts contacts: getAllContacts().values()) {
            ccc.addAll(contacts.getContacts());
        }
        Collections.shuffle(ccc, new SecureRandom());

        for (byte[] cipherMessage : cipherMessages.getData()) {
            AbstractBinaryDropMessage binMessage;
            byte binaryFormatVersion = cipherMessage[0];

            switch (binaryFormatVersion) {
                case 0:
                    try {
                        binMessage = new BinaryDropMessageV0(cipherMessage);
                    } catch (QblVersionMismatchException e) {
                        Log.e(TAG, "Version mismatch in binary drop message", e);
                        throw new RuntimeException("Version mismatch should not happen", e);
                    } catch (QblDropInvalidMessageSizeException e) {
                        Log.i(TAG, "Binary drop message version 0 with unexpected size discarded.");
                        // Invalid message uploads may happen with malicious intent
                        // or by broken clients. Skip.
                        continue;
                    }
                    break;
                default:
                    Log.w(TAG, "Unknown binary drop message version " + binaryFormatVersion);
                    // cannot handle this message -> skip
                    continue;
            }
            for (Identity identity : getIdentities().getIdentities()) {
                DropMessage dropMessage;
                try {
                    dropMessage = binMessage.disassembleMessage(identity);
                } catch (QblSpoofedSenderException e) {
                    //TODO: Notify the user about the spoofed message
                    break;
                }
                if (dropMessage != null) {
                    for (Contact c : ccc) {
                        if (c.getKeyIdentifier().equals(dropMessage.getSenderKeyId())) {
                            if (dropMessage.registerSender(c)) {
                                plainMessages.add(dropMessage);
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
        return plainMessages;
    }

    private Identities getIdentities() {
        try {
            return identityRepository.findAll();
        } catch (PersistenceException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<Identity, Contacts> getAllContacts() {
        try {
            Map<Identity, Contacts> contacts = new HashMap<>();
            for (Identity identity: getIdentities().getIdentities()) {
                contacts.put(identity, contactRepository.find(identity));
            }
            return contacts;
        } catch (PersistenceException e) {
            throw new IllegalStateException("Could not retrieve Identities", e);
        }
    }

    /**
     * Receives DropMessages via DropHTTP. Method extracted to mock receive in LocalQabelServiceTester.
     *
     * @param uri URI to receive DropMessages from
     * @return HTTPResult with collection of encrypted DropMessages.
     */
    HTTPResult<Collection<byte[]>> getDropMessages(URI uri, long sinceDate) {
        Log.v(TAG, "retrieveDropMessage: " + uri.toString() + " at: " + sinceDate);
        return dropHTTP.receiveMessages(uri, sinceDate);
    }
}
