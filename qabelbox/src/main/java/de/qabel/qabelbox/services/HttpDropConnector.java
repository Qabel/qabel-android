package de.qabel.qabelbox.services;

import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import javax.inject.Inject;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
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
                                @Nullable final OnSendDropMessageResult dropResultCallback)
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
                    HTTPResult<?> dropResult = dropHTTP.send(dropURL.getUri(), messageByteArray);
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
     * Retrieves all DropMessages for given Identities
     *
     * @return Retrieved, decrypted DropMessages.
     */
    @Override
    public RetrieveDropMessagesResult retrieveDropMessages(Identity identity, long sinceDate) {
        Collection<DropMessage> allMessages = new LinkedList<>();
        long resultSinceDate = sinceDate;
        try {
            Contacts identityContacts = contactRepository.find(identity);
            for (DropURL dropUrl : identity.getDropUrls()) {
                RetrieveDropMessagesResult result = retrieveDropMessages(identity, identityContacts, dropUrl.getUri(), sinceDate);
                allMessages.addAll(result.getMessages());
                resultSinceDate = Math.max(result.getSinceDate(), sinceDate);
            }
        } catch (PersistenceException | IOException e) {
            Log.e(TAG, "Error retrieving drop messages", e);
        }
        return new RetrieveDropMessagesResult(allMessages, resultSinceDate);
    }

    /**
     * Retrieves all DropMessages from given URI
     *
     * @param uri URI where to retrieve the drop from
     * @return Retrieved, decrypted DropMessages.
     */
    private RetrieveDropMessagesResult retrieveDropMessages(Identity identity, Contacts contacts, URI uri, long sinceDate) throws IOException {

        HTTPResult<Collection<byte[]>> cipherMessages = getDropMessages(uri, sinceDate);

        Collection<DropMessage> plainMessages = new LinkedList<>();

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
            try {
                DropMessage dropMessage = binMessage.disassembleMessage(identity);
                dropMessage.registerSender(contacts.getByKeyIdentifier(dropMessage.getSenderKeyId()));
                plainMessages.add(dropMessage);
            } catch (QblSpoofedSenderException e) {
                //TODO: Notify the user about the spoofed message
                break;
            }
        }

        return new RetrieveDropMessagesResult(plainMessages, cipherMessages.lastModified().getTime());
    }

    /**
     * Receives DropMessages via DropHTTP. Method extracted to mock receive in LocalQabelServiceTester.
     *
     * @param uri URI to receive DropMessages from
     * @return HTTPResult with collection of encrypted DropMessages.
     */
    private HTTPResult<Collection<byte[]>> getDropMessages(URI uri, long sinceDate) throws IOException {
        Log.v(TAG, "retrieveDropMessage: " + uri.toString() + " at: " + sinceDate);
        try {
            return dropHTTP.receiveMessages(uri, sinceDate);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
