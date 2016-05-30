package de.qabel.qabelbox.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import de.qabel.core.exceptions.QblInvalidEncryptionKeyException;
import de.qabel.core.exceptions.QblSpoofedSenderException;
import de.qabel.core.exceptions.QblVersionMismatchException;
import de.qabel.core.http.DropHTTP;
import de.qabel.core.http.HTTPResult;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.desktop.repository.sqlite.AndroidClientDatabase;
import de.qabel.qabelbox.exceptions.QblStorageEntityExistsException;
import de.qabel.qabelbox.helper.PreferencesHelper;
import de.qabel.qabelbox.persistence.AndroidPersistence;
import de.qabel.qabelbox.persistence.PersistenceMigration;
import de.qabel.qabelbox.persistence.QblSQLiteParams;
import de.qabel.qabelbox.persistence.RepositoryFactory;

public class LocalQabelService extends Service implements DropConnector {

    private final static Logger LOGGER = LoggerFactory.getLogger(LocalQabelService.class.getName());

    private static final String TAG = "LocalQabelService";
    public static final String PREF_LAST_ACTIVE_IDENTITY = "PREF_LAST_ACTIVE_IDENTITY";

    private final IBinder mBinder = new LocalBinder();

    protected static final String DB_NAME = "qabel-service";
    protected static final int DB_VERSION = 1;
    protected AndroidPersistence persistence;
    private DropHTTP dropHTTP;

    SharedPreferences sharedPreferences;
    private IdentityRepository identityRepository;
    private ContactRepository contactRepository;

    protected String getLastActiveIdentityID() {
        return sharedPreferences.getString(PREF_LAST_ACTIVE_IDENTITY, "");
    }

    public void addIdentity(Identity identity) {
        try {
            identityRepository.save(identity);
        } catch (PersistenceException e) {
            Log.e(TAG, "Could not save identity", e);
            throw new RuntimeException(e);
        }
    }

    public Identities getIdentities() {
        try {
            return identityRepository.findAll();
        } catch (PersistenceException e) {
            Log.e(TAG, "Could not get identities", e);
            throw new RuntimeException(e);
        }
    }

    public Identity getActiveIdentity() {
        String identityID = getLastActiveIdentityID();
        return getIdentities().getByKeyIdentifier(identityID);
    }

    public void setActiveIdentity(Identity identity) {
        PreferencesHelper.setLastActiveIdentityID(sharedPreferences, identity.getKeyIdentifier());
    }

    public void deleteIdentity(Identity identity) {
        try {
            identityRepository.delete(identity);
        } catch (PersistenceException e) {
            Log.e(TAG, "Could not delete identity");
        }
    }

    /**
     * Modify the identity in place
     *
     * @param identity known identity with modifid data
     */
    public void modifyIdentity(Identity identity) {
        try {
            identityRepository.save(identity);
        } catch (PersistenceException e) {
            Log.e(TAG, "Could not save identity", e);
        }
    }

    /**
     * Create a list of all contacts that are known, regardless of the identity that owns it
     *
     * @return List of all contacts
     */
    public Contacts getContacts() {
        return getContacts(getActiveIdentity());
    }

    /**
     * Reset the persistence
     * <p>
     * This should only be used in testing.
     */
    public void deleteContactsAndIdentities() {
        try {
            for (Identity identity : identityRepository.findAll().getIdentities()) {
                Set<Contact> contacts = contactRepository.find(identity).getContacts();
                for (Contact contact : contacts) {
                    try {
                        contactRepository.delete(contact, identity);
                    } catch (EntityNotFoundExcepion ignored) {
                    }
                }
                identityRepository.delete(identity);
            }
        } catch (PersistenceException e) {
            Log.e(TAG, "Error deleting contacts and identities");
        }
    }

    /**
     * Create a list of contacts for the given Identity
     *
     * @param identity selected identity
     * @return List of contacts owned by the identity
     */
    public Contacts getContacts(Identity identity) {
        if (identity == null) {
            return new Contacts(null);
        }
        try {
            return contactRepository.find(identity);
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
    }

    public void addContact(Contact contact) throws QblStorageEntityExistsException {
        addContact(contact, getActiveIdentity());
    }

    public void addContact(Contact contact, Identity identity) throws QblStorageEntityExistsException {
        try {
            contactRepository.findByKeyId(identity, contact.getKeyIdentifier());
            throw new QblStorageEntityExistsException("Contact exists");
        } catch (EntityNotFoundExcepion ignored) {
            try {
                contactRepository.save(contact, identity);
            } catch (PersistenceException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void deleteContact(Contact contact) {
        try {
            contactRepository.delete(contact, getActiveIdentity());
        } catch (EntityNotFoundExcepion | PersistenceException e) {
            throw new RuntimeException(e);
        }
    }

    public void modifyContact(Contact contact) {
        try {
            contactRepository.save(contact, getActiveIdentity());
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a map that maps each known identity to all of its contacts
     *
     * @return Map of each identity to its contacts
     */
    public Map<Identity, Contacts> getAllContacts() {
        Map<Identity, Contacts> contactMap = new HashMap<>();
        try {
            for (Identity identity : identityRepository.findAll().getIdentities()) {
                contactMap.put(identity, contactRepository.find(identity));
            }
        } catch (PersistenceException e) {
            throw new RuntimeException(e);
        }
        return contactMap;
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
        for (Contacts contacts : getAllContacts().values()) {
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
                        LOGGER.error("Version mismatch in binary drop message", e);
                        throw new RuntimeException("Version mismatch should not happen", e);
                    } catch (QblDropInvalidMessageSizeException e) {
                        LOGGER.info("Binary drop message version 0 with unexpected size discarded.");
                        // Invalid message uploads may happen with malicious intent
                        // or by broken clients. Skip.
                        continue;
                    }
                    break;
                default:
                    LOGGER.warn("Unknown binary drop message version " + binaryFormatVersion);
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

    public class LocalBinder extends Binder {
        public LocalQabelService getService() {
            // Return this instance of LocalQabelService so clients can call public methods
            return LocalQabelService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "LocalQabelService created");
        dropHTTP = new DropHTTP();
        initRepositories();
        try {
            migratePersistence();
        } catch (EntityNotFoundExcepion | PersistenceException e) {
            Log.e(TAG, "Migration of identities and contatcs failed", e);
        }
        sharedPreferences = getSharedPreferences(LocalQabelService.class.getCanonicalName(), MODE_PRIVATE);
    }

    private void migratePersistence() throws EntityNotFoundExcepion, PersistenceException {
        initAndroidPersistence();
        PersistenceMigration.migrate(persistence, identityRepository, contactRepository);
    }

    public AndroidPersistence getPersistence() {
        return persistence;
    }

    public void initRepositories() {
        RepositoryFactory repositoryFactory = new RepositoryFactory(getApplicationContext());
        AndroidClientDatabase androidClientDatabase = repositoryFactory.getAndroidClientDatabase();
        identityRepository = repositoryFactory.getIdentityRepository(androidClientDatabase);
        contactRepository = repositoryFactory.getContactRepository(androidClientDatabase);
    }

    protected void initAndroidPersistence() {
        initAndroidPersistence(DB_NAME);
    }

    protected void initAndroidPersistence(String dbName) {
        AndroidPersistence androidPersistence;
        QblSQLiteParams params = new QblSQLiteParams(getApplicationContext(), dbName, null, DB_VERSION);
        try {
            androidPersistence = new AndroidPersistence(params);
        } catch (QblInvalidEncryptionKeyException e) {
            Log.e(TAG, "Invalid database password!");
            return;
        }
        this.persistence = androidPersistence;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}

