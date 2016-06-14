package de.qabel.qabelbox.storage;

import android.support.annotation.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import de.qabel.core.config.Identity;
import de.qabel.core.crypto.CryptoUtils;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.storage.model.BoxFile;
import de.qabel.qabelbox.storage.model.BoxUploadingFile;
import de.qabel.qabelbox.storage.transfer.BoxTransferListener;

public interface BoxManager {

    String BLOCKS_PREFIX = "blocks/";

    CryptoUtils getCryptoUtils();

    @Nullable
    Collection<BoxFile> getCachedFinishedUploads(String path);
    void clearCachedUploads(String path);
    List<BoxUploadingFile> getPendingUploads(String path);

    BoxVolume createBoxVolume(String identity, String prefix) throws QblStorageException;
    BoxVolume createBoxVolume(Identity identity) throws QblStorageException;
    void notifyBoxVolumesChanged();

    File downloadFileDecrypted(String documentId) throws QblStorageException;
    InputStream downloadStreamDecrypted(BoxFile boxFile,  String identityKeyIdentifier, String path) throws QblStorageException;
    File downloadFileDecrypted(BoxFile boxFile, String identityKeyIdentifier, String path) throws QblStorageException;
    File blockingDownload(String prefix, String name, BoxTransferListener boxTransferListener) throws QblStorageException;
    File downloadDecrypted(String prefix, String name, byte[] key, BoxTransferListener boxTransferListener) throws QblStorageException;

    void blockingUpload(String prefix, String name, InputStream inputStream) throws QblStorageException;
    BoxFile uploadEncrypted(String documentIdString, InputStream content) throws QblStorageException;
    BoxFile uploadEncrypted(String documentIdString, File content) throws QblStorageException;
    void uploadEncrypted(String prefix, String block, byte[] key,
                         InputStream content, BoxTransferListener boxTransferListener) throws QblStorageException;

    void delete(String prefix, String ref) throws QblStorageException;
}
