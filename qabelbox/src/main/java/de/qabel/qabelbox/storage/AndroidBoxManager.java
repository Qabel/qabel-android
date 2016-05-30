package de.qabel.qabelbox.storage;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;

import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.qabelbox.QblBroadcastConstants;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.providers.DocumentIdParser;
import de.qabel.qabelbox.services.StorageBroadcastConstants;
import de.qabel.qabelbox.storage.model.BoxFile;
import de.qabel.qabelbox.storage.model.BoxUploadingFile;
import de.qabel.qabelbox.storage.notifications.StorageNotificationManager;
import de.qabel.qabelbox.storage.transfer.BoxTransferListener;
import de.qabel.qabelbox.storage.transfer.TransferManager;

public class AndroidBoxManager implements BoxManager {

    Context context;
    StorageNotificationManager storageNotificationManager;
    DocumentIdParser documentIdParser;
    IdentityRepository identityRepository;
    AppPreference appPreferences;
    TransferManager transferManager;

    //TODO Queue is currently not used!
    private static Queue<BoxUploadingFile> uploadingQueue = new LinkedBlockingQueue<>();
    private static Map<String, Map<String, BoxFile>> cachedFinishedUploads =
            Collections.synchronizedMap(new HashMap<>());

    @Inject
    public AndroidBoxManager(Context context,
                             StorageNotificationManager notificationManager,
                             DocumentIdParser documentIdParser,
                             AppPreference preferences,
                             TransferManager transferManager,
                             IdentityRepository identityRepository) {
        this.context = context;
        this.storageNotificationManager = notificationManager;
        this.documentIdParser = documentIdParser;
        this.appPreferences = preferences;
        this.transferManager = transferManager;
        this.identityRepository = identityRepository;
    }

    @Override
    @Nullable
    public Collection<BoxFile> getCachedFinishedUploads(String path) {
        Map<String, BoxFile> files = this.cachedFinishedUploads.get(path);
        if (files != null) {
            return files.values();
        }
        return null;
    }

    @Override
    public void clearCachedUploads(String path) {
        this.cachedFinishedUploads.remove(path);
    }

    @Override
    public List<BoxUploadingFile> getPendingUploads(String path) {
        List<BoxUploadingFile> uploadingFiles = new LinkedList<>();
        for (BoxUploadingFile f : uploadingQueue) {
            if (f.getPath().equals(path)) {
                uploadingFiles.add(f);
            }
        }
        return uploadingFiles;
    }

    @Override
    public BoxTransferListener addUploadTransfer(String documentId) throws QblStorageException {
        try {
            String uploadPath = documentIdParser.getPath(documentId);
            String filename = documentIdParser.getBaseName(documentId);

            final BoxUploadingFile boxUploadingFile = new BoxUploadingFile(filename, uploadPath,
                    documentIdParser.getIdentity(documentId));

            uploadingQueue.add(boxUploadingFile);
            updateUploadNotifications();
            broadcastUploadStatus(documentId, StorageBroadcastConstants.UPLOAD_STATUS_NEW);
            return new BoxTransferListener() {
                @Override
                public void onProgressChanged(long bytesCurrent, long bytesTotal) {
                    boxUploadingFile.totalSize = bytesTotal;
                    boxUploadingFile.uploadedSize = bytesCurrent;
                    updateUploadNotifications();
                }

                @Override
                public void onFinished() {
                    boxUploadingFile.uploadedSize = boxUploadingFile.totalSize;
                    updateUploadNotifications();
                }
            };
        } catch (FileNotFoundException e) {
            throw new QblStorageException(e);
        }
    }

    private void broadcastUploadStatus(String documentId, int uploadStatus) {
        Intent intent = new Intent(QblBroadcastConstants.Storage.BOX_UPLOAD_CHANGED);
        intent.putExtra(StorageBroadcastConstants.EXTRA_UPLOAD_DOCUMENT_ID, documentId);
        intent.putExtra(StorageBroadcastConstants.EXTRA_UPLOAD_STATUS, uploadStatus);
        context.sendBroadcast(intent);
    }


    private void updateUploadNotifications() {
        storageNotificationManager.updateUploadNotification(uploadingQueue.size(), uploadingQueue.peek());
    }

    @Override
    public void removeUpload(String documentId, int cause, @Nullable BoxFile resultFile) throws QblStorageException {
        try {
            BoxUploadingFile uploadingFile = uploadingQueue.poll();
            String uploadPath = documentIdParser.getPath(documentId);
            switch (cause) {
                case StorageBroadcastConstants.UPLOAD_STATUS_FINISHED:
                    cacheFinishedUpload(documentId, resultFile);
                    break;
            }
            updateUploadNotifications();
            broadcastUploadStatus(documentId, cause);
        } catch (FileNotFoundException e) {
            throw new QblStorageException(e);
        }
    }

    public void cacheFinishedUpload(String documentId, BoxFile boxFile) {
        try {
            Map<String, BoxFile> cachedFiles = cachedFinishedUploads.get(documentIdParser.getPath(documentId));
            if (cachedFiles == null) {
                cachedFiles = new HashMap<>();
            }
            cachedFiles.put(boxFile.name, boxFile);
            cachedFinishedUploads.put(documentIdParser.getPath(documentId), cachedFiles);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public BoxVolume createBoxVolume(String identity, String prefix) throws QblStorageException {
        try {
            Identity retrievedIdentity = identityRepository.find(identity);
            if (retrievedIdentity == null) {
                throw new RuntimeException("Identity " + identity + "is unknown!");
            }
            QblECKeyPair key = retrievedIdentity.getPrimaryKeyPair();

            byte[] deviceId = appPreferences.getDeviceId();
            return new BoxVolume(key, prefix, deviceId, context, transferManager);
        } catch (EntityNotFoundExcepion | PersistenceException e) {
            throw new QblStorageException("Cannot create BoxVolume");
        }
    }

    @Override
    public BoxVolume createBoxVolume(Identity identity) throws QblStorageException {
        return createBoxVolume(identity.getKeyIdentifier(), identity.getPrefixes().get(0));
    }

    @Override
    public void notifyBoxChanged() {
        context.sendBroadcast(new Intent(QblBroadcastConstants.Storage.BOX_VOLUMES_CHANGES));
    }
}
