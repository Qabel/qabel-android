package de.qabel.qabelbox.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.AbstractBinaryDropMessage;
import de.qabel.core.crypto.BinaryDropMessageV0;
import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropInvalidMessageSizeException;
import de.qabel.core.exceptions.QblDropPayloadSizeException;
import de.qabel.core.exceptions.QblInvalidEncryptionKeyException;
import de.qabel.core.exceptions.QblSpoofedSenderException;
import de.qabel.core.exceptions.QblVersionMismatchException;
import de.qabel.core.http.DropHTTP;
import de.qabel.core.http.HTTPResult;
import de.qabel.desktop.config.factory.DefaultIdentityFactory;
import de.qabel.desktop.repository.EntityManager;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.desktop.repository.sqlite.AndroidClientDatabase;
import de.qabel.desktop.repository.sqlite.DesktopClientDatabase;
import de.qabel.desktop.repository.sqlite.MigrationException;
import de.qabel.desktop.repository.sqlite.SqliteDropUrlRepository;
import de.qabel.desktop.repository.sqlite.SqliteIdentityRepository;
import de.qabel.desktop.repository.sqlite.SqlitePrefixRepository;
import de.qabel.desktop.repository.sqlite.hydrator.DropURLHydrator;
import de.qabel.desktop.repository.sqlite.hydrator.IdentityHydrator;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.exceptions.QblStorageEntityExistsException;
import de.qabel.qabelbox.persistence.AndroidPersistence;
import de.qabel.qabelbox.persistence.QblSQLiteParams;
import de.qabel.qabelbox.persistence.RepositoryFactory;
import de.qabel.qabelbox.providers.DocumentIdParser;
import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxUploadingFile;
import de.qabel.qabelbox.storage.TransferManager;

public class LocalQabelService extends Service implements DropConnector {

    private final static Logger LOGGER = LoggerFactory.getLogger(LocalQabelService.class.getName());

    private static final String TAG = "LocalQabelService";
    private static final String PREF_LAST_ACTIVE_IDENTITY = "PREF_LAST_ACTIVE_IDENTITY";
    public static final String DEFAULT_DROP_SERVER = "http://localhost";

    private static final String PREF_DEVICE_ID_CREATED = "PREF_DEVICE_ID_CREATED";
    private static final String PREF_DEVICE_ID = "PREF_DEVICE_ID";
    private static final int NUM_BYTES_DEVICE_ID = 16;
    private static final int UPLOAD_NOTIFICATION_ID = 162134;

    private final IBinder mBinder = new LocalBinder();

    protected static final String DB_NAME = "qabel-service";
    protected static final int DB_VERSION = 1;
    protected AndroidPersistence persistence;
    private DropHTTP dropHTTP;
    private HashMap<String, Map<String, BoxUploadingFile>> pendingUploads;
    private Queue<BoxUploadingFile> uploadingQueue;
    private Map<String, Map<String, BoxFile>> cachedFinishedUploads;
    private DocumentIdParser documentIdParser;

    SharedPreferences sharedPreferences;
    private IdentityRepository identityRepository;

    protected void setLastActiveIdentityID(String identityID) {
        sharedPreferences.edit()
                .putString(PREF_LAST_ACTIVE_IDENTITY, identityID)
                .apply();
    }

    protected String getLastActiveIdentityID() {
        return sharedPreferences.getString(PREF_LAST_ACTIVE_IDENTITY, "");
    }

    public void addIdentity(Identity identity) {
        persistence.updateOrPersistEntity(identity);
        try {
            identityRepository.save(identity);
        } catch (PersistenceException e) {
            Log.e(TAG, "Could not save identity", e);
        }
    }

    public Identities getIdentities() {
        try {
            return identityRepository.findAll();
        } catch (EntityNotFoundExcepion | PersistenceException e) {
            Log.e(TAG, "Could not get identities", e);
            throw new RuntimeException(e);
        }
    }

    public Identity getActiveIdentity() {
        String identityID = getLastActiveIdentityID();
        return getIdentities().getByKeyIdentifier(identityID);
    }

    public void setActiveIdentity(Identity identity) {
        setLastActiveIdentityID(identity.getKeyIdentifier());
    }

    public void deleteIdentity(Identity identity) {
        persistence.removeEntity(identity.getPersistenceID(), Identity.class);
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
        persistence.updateEntity(identity);
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
     *
     * This should only be used in testing.
     */
    public void deleteContactsAndIdentities() {
        persistence.dropTable(Contacts.class);
        persistence.dropTable(Identity.class);
    }

    /**
     * Create a list of contacts for the given Identity
     *
     * @param identity selected identity
     * @return List of contacts owned by the identity
     */
    public Contacts getContacts(Identity identity) {
        List<Contacts> entities = persistence.getEntities(Contacts.class);
        for (Contacts contacts : entities) {
            Identity owner = contacts.getIdentity();
            if (owner.equals(identity)) {
                return contacts;
            }
        }
        Contacts contacts = new Contacts(identity);
        persistence.updateOrPersistEntity(contacts);
        return contacts;
    }

    public void addContact(Contact contact) throws QblStorageEntityExistsException {
        Contacts contacts = getContacts();
        if (contacts.contains(contact)) {
            throw new QblStorageEntityExistsException(contact.getAlias());
        }
        contacts.put(contact);
        persistence.updateEntity(contacts);
    }

    public void addContact(Contact contact, Identity identity) {
        Contacts contacts = getContacts(identity);
        contacts.put(contact);
        persistence.updateEntity(contacts);
    }

    public void deleteContact(Contact contact) {
        Contacts contacts = getContacts();
        contacts.remove(contact);
        persistence.updateEntity(contacts);
    }

    public void deleteContact(Contact contact, Identity identity) {
        Contacts contacts = getContacts(identity);
        contacts.remove(contact);
        persistence.updateEntity(contacts);
    }

    public void modifyContact(Contact contact) {
        Contacts contacts = getContacts();
        contacts.remove(contact);
        contacts.put(contact);
        persistence.updateEntity(contacts);
    }

    public void modifyContact(Contact contact, Identity identity) {
        Contacts contacts = getContacts(identity);
        contacts.remove(contact);
        contacts.put(contact);
        persistence.updateEntity(contacts);
    }

    /**
     * Create a map that maps each known identity to all of its contacts
     *
     * @return Map of each identity to its contacts
     */
    public Map<Identity, Contacts> getAllContacts() {
        Map<Identity, Contacts> contactMap = new HashMap<>();
        for (Contacts contacts : persistence.getEntities(Contacts.class)) {
            contactMap.put(contacts.getIdentity(), contacts);
        }
        return contactMap;
    }

    public interface OnSendDropMessageResult {
        void onSendDropResult(Map<DropURL, Boolean> deliveryStatus);
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

    public byte[] getDeviceID() {
        String deviceID = sharedPreferences.getString(PREF_DEVICE_ID, "");
        if (deviceID.equals("")) {
            // Should never occur
            throw new RuntimeException("DeviceID not created!");
        }
        return Hex.decode(deviceID);
    }

    public HashMap<String, Map<String, BoxUploadingFile>> getPendingUploads() {
        return pendingUploads;
    }

    public BoxUploadingFile addPendingUpload(String documentId, Bundle extras) throws FileNotFoundException {
        String uploadPath = documentIdParser.getPath(documentId);
        String filename = documentIdParser.getBaseName(documentId);
        Map<String, BoxUploadingFile> uploadsInPath = pendingUploads.get(uploadPath);
        if (uploadsInPath == null) {
            uploadsInPath = new HashMap<>();
        }
        BoxUploadingFile boxUploadingFile = new BoxUploadingFile(filename);
        uploadsInPath.put(filename, boxUploadingFile);
        pendingUploads.put(uploadPath, uploadsInPath);
        uploadingQueue.add(boxUploadingFile);
        updateNotification();
        broadcastUploadStatus(documentId, LocalBroadcastConstants.UPLOAD_STATUS_NEW, extras);
        return boxUploadingFile;
    }

    public boolean removePendingUpload(String documentId, int cause, @Nullable Bundle extras) throws FileNotFoundException {
        String uploadPath = documentIdParser.getPath(documentId);
        Map<String, BoxUploadingFile> uploadsInPath = pendingUploads.get(uploadPath);
        switch (cause) {
            case LocalBroadcastConstants.UPLOAD_STATUS_FINISHED:
                broadcastUploadStatus(documentId, LocalBroadcastConstants.UPLOAD_STATUS_FINISHED, extras);
                cacheFinishedUpload(documentId, extras);
                break;
            case LocalBroadcastConstants.UPLOAD_STATUS_FAILED:
                broadcastUploadStatus(documentId, LocalBroadcastConstants.UPLOAD_STATUS_FAILED, extras);
                break;
        }
        return uploadsInPath != null && uploadsInPath.remove(documentIdParser.getBaseName(documentId)) != null;
    }

    private void cacheFinishedUpload(String documentId, Bundle extras) {
        if (extras != null) {
            BoxFile boxFile = extras.getParcelable(LocalBroadcastConstants.EXTRA_FILE);
            if (boxFile != null) {
                try {
                    Map<String, BoxFile> cachedFiles = cachedFinishedUploads.get(documentIdParser.getPath(documentId));
                    if (cachedFiles == null) {
                        cachedFiles = new HashMap<>();
                    }
                    cachedFiles.remove(boxFile.name);
                    cachedFiles.put(boxFile.name, boxFile);
                    cachedFinishedUploads.put(documentIdParser.getPath(documentId), cachedFiles);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void updateNotification() {
        BoxUploadingFile boxUploadingFile = uploadingQueue.peek();
        if (boxUploadingFile != null) {
            showNotification(getResources().getQuantityString(R.plurals.uploadsNotificationTitle,
                    uploadingQueue.size(), uploadingQueue.size()),
                    String.format(getString(R.string.upload_in_progress_notification_content), boxUploadingFile.name),
                    boxUploadingFile.getUploadStatusPercent());
        } else {
            showNotification((getString(R.string.upload_complete_notification_title)), null, 100);
        }
    }

    protected void showNotification(String contentTitle, @Nullable String contentText, int progress) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.qabel_logo)
                .setProgress(100, progress, false)
                .setContentIntent(intent);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifyManager.notify(UPLOAD_NOTIFICATION_ID, builder.build());
    }

    public TransferManager.BoxTransferListener getUploadTransferListener(final BoxUploadingFile boxUploadingFile) {
        return new TransferManager.BoxTransferListener() {
            @Override
            public void onProgressChanged(long bytesCurrent, long bytesTotal) {
                boxUploadingFile.totalSize = bytesTotal;
                boxUploadingFile.uploadedSize = bytesCurrent;
                updateNotification();
            }

            @Override
            public void onFinished() {
                uploadingQueue.remove(boxUploadingFile);
                updateNotification();
            }
        };
    }

    protected void broadcastUploadStatus(String documentId, int uploadStatus, @Nullable Bundle extras) {
        Intent intent = new Intent(LocalBroadcastConstants.INTENT_UPLOAD_BROADCAST);
        intent.putExtra(LocalBroadcastConstants.EXTRA_UPLOAD_DOCUMENT_ID, documentId);
        intent.putExtra(LocalBroadcastConstants.EXTRA_UPLOAD_STATUS, uploadStatus);
        if (extras != null) {
            intent.putExtra(LocalBroadcastConstants.EXTRA_UPLOAD_EXTRA, extras);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public Map<String, Map<String, BoxFile>> getCachedFinishedUploads() {
        return cachedFinishedUploads;
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
        initSharedPreferences();
        initAndroidPersistence();
        pendingUploads = new HashMap<>();
        documentIdParser = new DocumentIdParser();
        cachedFinishedUploads = Collections.synchronizedMap(new HashMap<>());
        uploadingQueue = new LinkedBlockingDeque<>();
        RepositoryFactory repositoryFactory = new RepositoryFactory(getApplicationContext());
        try {
            AndroidClientDatabase androidClientDatabase = repositoryFactory.getAndroidClientDatabase();
            androidClientDatabase.migrate();
            identityRepository = repositoryFactory.getIdentityRepository(androidClientDatabase);
        } catch (SQLException | MigrationException e) {
            throw new RuntimeException(e);
        }

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

    protected void initSharedPreferences() {
        sharedPreferences = getSharedPreferences(this.getClass().getCanonicalName(), MODE_PRIVATE);
        if (!sharedPreferences.getBoolean(PREF_DEVICE_ID_CREATED, false)) {

            CryptoUtils cryptoUtils = new CryptoUtils();
            byte[] deviceID = cryptoUtils.getRandomBytes(NUM_BYTES_DEVICE_ID);

            Log.d(this.getClass().getName(), "New device ID: " + Hex.toHexString(deviceID));

            sharedPreferences.edit().putString(PREF_DEVICE_ID, Hex.toHexString(deviceID))
                    .putBoolean(PREF_DEVICE_ID_CREATED, true)
                    .apply();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}

