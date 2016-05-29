package de.qabel.qabelbox.storage;

import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.List;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.storage.model.BoxFile;
import de.qabel.qabelbox.storage.model.BoxUploadingFile;
import de.qabel.qabelbox.storage.transfer.BoxTransferListener;

public interface BoxManager {

    @Nullable
    Collection<BoxFile> getCachedFinishedUploads(String path);

    void clearCachedUploads(String path);

    List<BoxUploadingFile> getPendingUploads(String path);

    BoxTransferListener addUploadTransfer(String documentId) throws QblStorageException;
    void removeUpload(String documentId, int cause, @Nullable BoxFile resultFile) throws QblStorageException;

    BoxVolume createBoxVolume(String identity, String prefix) throws QblStorageException;
    BoxVolume createBoxVolume(Identity identity) throws QblStorageException;
    void notifyBoxChanged();

}
