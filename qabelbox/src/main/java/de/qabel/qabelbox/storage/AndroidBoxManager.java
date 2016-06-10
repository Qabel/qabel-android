package de.qabel.qabelbox.storage;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;

import de.qabel.core.config.Identity;
import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.qabelbox.QblBroadcastConstants;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblServerException;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.exceptions.QblStorageNotFound;
import de.qabel.qabelbox.providers.DocumentId;
import de.qabel.qabelbox.providers.DocumentIdParser;
import de.qabel.qabelbox.services.StorageBroadcastConstants;
import de.qabel.qabelbox.storage.model.BoxFile;
import de.qabel.qabelbox.storage.model.BoxUploadingFile;
import de.qabel.qabelbox.storage.navigation.BoxNavigation;
import de.qabel.qabelbox.storage.notifications.StorageNotificationManager;
import de.qabel.qabelbox.storage.transfer.BoxTransferListener;
import de.qabel.qabelbox.storage.transfer.TransferManager;

public class AndroidBoxManager implements BoxManager {

    private final FileCache fileCache;
    private final CryptoUtils cryptoUtils;

    private class UploadResult {
        protected long mTime;
        protected long size;

        protected UploadResult(long mTime, long size) {
            this.mTime = mTime;
            this.size = size;
        }
    }

    Context context;
    StorageNotificationManager storageNotificationManager;
    DocumentIdParser documentIdParser;
    IdentityRepository identityRepository;
    AppPreference appPreferences;
    TransferManager transferManager;

    //TODO Queue is currently not used!
    private static Queue<BoxUploadingFile> uploadingQueue = new LinkedBlockingQueue<>();
    private static Map<String, Map<String, BoxFile>> cachedFinishedUploads =
            Collections.synchronizedMap(new HashMap<String, Map<String, BoxFile>>());

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
        this.fileCache = new FileCache(context);
        this.cryptoUtils = new CryptoUtils();
    }

    @Override
    public CryptoUtils getCryptoUtils() {
        return cryptoUtils;
    }

    @Override
    @Nullable
    public Collection<BoxFile> getCachedFinishedUploads(String path) {
        Map<String, BoxFile> files = cachedFinishedUploads.get(path);
        if (files != null) {
            return files.values();
        }
        return null;
    }

    @Override
    public void clearCachedUploads(String path) {
        cachedFinishedUploads.remove(path);
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

    protected BoxTransferListener addUploadTransfer(DocumentId documentId) throws QblStorageException {

        final BoxUploadingFile boxUploadingFile = new BoxUploadingFile(documentId.getFileName(),
                documentId.getPathString(), documentId.getIdentityKey());

        uploadingQueue.add(boxUploadingFile);
        updateUploadNotifications();
        broadcastUploadStatus(documentId.toString(), StorageBroadcastConstants.UPLOAD_STATUS_NEW);

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

    private void removeUpload(String documentId, int cause, @Nullable BoxFile resultFile) throws QblStorageException {
        uploadingQueue.poll();
        switch (cause) {
            case StorageBroadcastConstants.UPLOAD_STATUS_FINISHED:
                cacheFinishedUpload(documentId, resultFile);
                break;
        }
        updateUploadNotifications();
        broadcastUploadStatus(documentId, cause);
    }

    private void cacheFinishedUpload(String documentId, BoxFile boxFile) {
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
            return new BoxVolume(key, prefix, deviceId, context, this);
        } catch (EntityNotFoundExcepion | PersistenceException e) {
            throw new QblStorageException("Cannot create BoxVolume");
        }
    }

    @Override
    public BoxVolume createBoxVolume(Identity identity) throws QblStorageException {
        return createBoxVolume(identity.getKeyIdentifier(), identity.getPrefixes().get(0));
    }

    @Override
    public void notifyBoxVolumesChanged() {
        context.sendBroadcast(new Intent(QblBroadcastConstants.Storage.BOX_VOLUMES_CHANGES));
    }

    private void notifyBoxChanged() {
        context.sendBroadcast(new Intent(QblBroadcastConstants.Storage.BOX_CHANGED));
    }

    @Override
    public File downloadFileDecrypted(String documentIdString) throws QblStorageException {
        DocumentId documentId = documentIdParser.parse(documentIdString);

        BoxVolume volume = createBoxVolume(documentId.getIdentityKey(), documentId.getPrefix());
        BoxNavigation navigation = volume.navigate();
        navigation.navigate(documentId.getFilePath());

        BoxFile file = navigation.getFile(documentId.getFileName(), true);
        return downloadFileDecrypted(file, documentId.getIdentityKey(), documentId.getPathString());
    }

    @Override
    public InputStream downloadStreamDecrypted(BoxFile boxFile, String identityKeyIdentifier, String path) throws QblStorageException {
        try {
            File file = downloadFileDecrypted(boxFile, identityKeyIdentifier, path);
            return new FileInputStream(file);
        } catch (IOException e) {
            throw new QblStorageException(e);
        }
    }

    @Override
    public File downloadFileDecrypted(BoxFile boxFile, String identityKeyIdentifier, String path) throws QblStorageException {
        File file = fileCache.get(boxFile);
        if (file != null) {
            return file;
        }

        File downloadedFile = blockingDownload(boxFile.prefix,
                BLOCKS_PREFIX + boxFile.block,
                storageNotificationManager.addDownloadNotification(identityKeyIdentifier, path, boxFile));

        File outputFile = new File(context.getExternalCacheDir(), boxFile.name);
        decryptFile(boxFile.key, downloadedFile, outputFile);
        fileCache.put(boxFile, outputFile);

        return outputFile;
    }

    @Override
    public File blockingDownload(String prefix, String name, BoxTransferListener boxTransferListener) throws QblStorageException {
        File target = transferManager.createTempFile();
        int id = transferManager.download(prefix, name, target, boxTransferListener);
        if (transferManager.waitFor(id)) {
            return target;
        } else {
            try {
                throw transferManager.lookupError(id);
            } catch (QblServerException e) {
                if (e.getStatusCode() == 404) {
                    throw new QblStorageNotFound("File not found. Prefix: " + prefix + " Name: " + name);
                }
                throw new QblStorageException(e);
            } catch (Exception e) {
                throw new QblStorageException(e);
            }
        }
    }

    private void decryptFile(byte[] boxFileKey, File sourceFile, File targetFile) throws QblStorageException {
        KeyParameter key = new KeyParameter(boxFileKey);
        try {
            if (!cryptoUtils.decryptFileAuthenticatedSymmetricAndValidateTag(
                    new FileInputStream(sourceFile), targetFile, key)
                    || targetFile.length() == 0) {
                throw new QblStorageException("Decryption failed");
            }
        } catch (IOException | InvalidKeyException e) {
            throw new QblStorageException(e);
        }
    }

    @Override
    public File downloadDecrypted(String prefix, String name, byte[] key, BoxTransferListener boxTransferListener) throws QblStorageException {
        File downloadedFile = blockingDownload(prefix, name, boxTransferListener);
        File outputFile = transferManager.createTempFile();
        decryptFile(key, downloadedFile, outputFile);
        return outputFile;
    }

    @Override
    public void blockingUpload(String prefix, String name, InputStream inputStream) throws QblStorageException {
        try {
            File tmpFile = transferManager.createTempFile();
            IOUtils.copy(inputStream, new FileOutputStream(tmpFile));
            blockingUpload(prefix, name, tmpFile, null);
        } catch (IOException e) {
            throw new QblStorageException(e);
        }
    }

    protected long blockingUpload(String prefix, String name,
                                  File file, BoxTransferListener boxTransferListener) throws QblStorageException {
        int id = transferManager.uploadAndDeleteLocalfileOnSuccess(prefix, name, file, boxTransferListener);
        if (!transferManager.waitFor(id)) {
            throw new QblStorageException("Upload failed!");
        }
        return currentSecondsFromEpoch();
    }

    private long currentSecondsFromEpoch() {
        return System.currentTimeMillis() / 1000;
    }

    protected UploadResult uploadEncrypted(
            InputStream content, KeyParameter key, String prefix, String block,
            BoxTransferListener boxTransferListener) throws QblStorageException {
        try {
            File tempFile = transferManager.createTempFile();
            OutputStream outputStream = new FileOutputStream(tempFile);
            if (!cryptoUtils.encryptStreamAuthenticatedSymmetric(content, outputStream, key, null)) {
                throw new QblStorageException("Encryption failed");
            }
            outputStream.flush();
            Long size = tempFile.length();
            Long mTime = blockingUpload(prefix, block, tempFile, boxTransferListener);
            return new UploadResult(mTime, size);
        } catch (IOException | InvalidKeyException e) {
            throw new QblStorageException(e);
        }
    }

    @Override
    public BoxFile uploadEncrypted(String documentIdString, InputStream content) throws QblStorageException {
        DocumentId documentId = documentIdParser.parse(documentIdString);

        KeyParameter key = cryptoUtils.generateSymmetricKey();
        String block = UUID.randomUUID().toString();

        BoxTransferListener boxTransferListener = addUploadTransfer(documentId);
        try {
            UploadResult uploadResult = uploadEncrypted(content, key, documentId.getPrefix(),
                    BLOCKS_PREFIX + block, boxTransferListener);

            BoxFile boxResult = new BoxFile(documentId.getPrefix(), block,
                    documentId.getFileName(), uploadResult.size, uploadResult.mTime, key.getKey());

            removeUpload(documentIdString, StorageBroadcastConstants.UPLOAD_STATUS_FINISHED, boxResult);
            return boxResult;
        } catch (QblStorageException e) {
            removeUpload(documentIdString, StorageBroadcastConstants.UPLOAD_STATUS_FAILED, null);
            throw e;
        } finally {
            notifyBoxChanged();
        }
    }

    @Override
    public BoxFile uploadEncrypted(String documentIdString, File content) throws QblStorageException {
        try {
            return uploadEncrypted(documentIdString, new FileInputStream(content));
        } catch (FileNotFoundException e) {
            throw new QblStorageException(e);
        }
    }

    @Override
    public void uploadEncrypted(String prefix, String block, byte[] key,
                                InputStream content, BoxTransferListener boxTransferListener) throws QblStorageException {
        uploadEncrypted(content, new KeyParameter(key), prefix, block, boxTransferListener);
    }

    @Override
    public void delete(String prefix, String ref) throws QblStorageException {
        int requestId = transferManager.delete(prefix, ref);
        this.fileCache.remove(ref);
        if (!transferManager.waitFor(requestId)) {
            throw new QblStorageException("Cannot delete file!");
        }
        notifyBoxChanged();
    }

}
