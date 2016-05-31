package de.qabel.qabelbox.storage;

import android.support.annotation.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.storage.model.BoxFile;
import de.qabel.qabelbox.storage.model.BoxUploadingFile;
import de.qabel.qabelbox.storage.transfer.BoxTransferListener;

public interface BoxManager {

    String BLOCKS_PREFIX = "blocks/";

    @Nullable
    Collection<BoxFile> getCachedFinishedUploads(String path);

    void clearCachedUploads(String path);

    List<BoxUploadingFile> getPendingUploads(String path);

    BoxVolume createBoxVolume(String identity, String prefix) throws QblStorageException;
    BoxVolume createBoxVolume(Identity identity) throws QblStorageException;
    void notifyBoxVolumesChanged();

    File downloadFile(String documentId) throws QblStorageException;
    InputStream downloadStream(BoxFile boxFile, BoxTransferListener boxTransferListener) throws QblStorageException;
    File downloadFile(BoxFile boxFile, BoxTransferListener boxTransferListener) throws QblStorageException;
    File blockingDownload(String prefix, String name, BoxTransferListener boxTransferListener) throws QblStorageException;

    BoxFile upload(String documentIdString, InputStream content) throws QblStorageException;

    void upload(String prefix, String block, byte[] key,
                InputStream content, BoxTransferListener boxTransferListener) throws QblStorageException;
}
