package de.qabel.qabelbox.storage.navigation;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.util.Stack;

import de.qabel.core.crypto.DecryptedPlaintext;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.helper.FileHelper;
import de.qabel.qabelbox.storage.BoxManager;
import de.qabel.qabelbox.storage.BoxVolume;
import de.qabel.qabelbox.storage.DirectoryMetadata;
import de.qabel.qabelbox.storage.model.BoxFolder;

public class FolderNavigation extends AbstractNavigation {

    private static final String TAG = FolderNavigation.class.getName();

    public FolderNavigation(String prefix, DirectoryMetadata dm, QblECKeyPair keyPair, @Nullable byte[] dmKey, byte[] deviceId,
                            BoxManager boxManager, BoxVolume boxVolume, String path,
                            @Nullable Stack<BoxFolder> parents, Context context) {
        super(prefix, dm, keyPair, dmKey, deviceId, boxManager, boxVolume, path, parents, context);
    }

    @Override
    protected void uploadDirectoryMetadata() throws QblStorageException {
        if (currentPath.equals("/")) {
            uploadDirectoryMetadataRoot();
        } else {
            uploadDirectoryMetadataSubFolder();
        }
    }

    private void uploadDirectoryMetadataRoot() throws QblStorageException {
        Log.d(TAG, "Uploading directory metadata root (" + dm.getFileName() +")");
        try {
            byte[] plaintext = FileHelper.toByteArray(new FileInputStream(dm.getPath()));
            byte[] encrypted = boxManager.getCryptoUtils().
                    createBox(keyPair, keyPair.getPub(), plaintext, 0);
            boxManager.blockingUpload(prefix, dm.getFileName(), new ByteArrayInputStream(encrypted));
        } catch (IOException | InvalidKeyException e) {
            throw new QblStorageException(e);
        }
    }

    private void uploadDirectoryMetadataSubFolder() throws QblStorageException {
        Log.d(TAG, "Uploading directory metadata (" + dm.getFileName() +")");
        try {
            boxManager.uploadEncrypted(prefix, dm.getFileName(), dmKey, new FileInputStream(dm.getPath()), null);
        } catch (FileNotFoundException e) {
            throw new QblStorageException(e);
        }
    }

    protected DirectoryMetadata reloadMetadata() throws QblStorageException {
        if (currentPath.equals("/")) {
            return reloadMetadataRoot();
        } else {
            return reloadMetadataSubFolder();
        }
    }

    protected DirectoryMetadata reloadMetadataSubFolder() throws QblStorageException {
        Log.d(TAG, "Reloading directory metadata (" + dm.getFileName()+")");
        // duplicate of navigate()
        File indexDl = boxManager.downloadDecrypted(prefix, dm.getFileName(), dmKey, null);
        return DirectoryMetadata.openDatabase(indexDl, deviceId, dm.getFileName(), dm.getTempDir());
    }

    protected DirectoryMetadata reloadMetadataRoot() throws QblStorageException {
        Log.d(TAG, "Reloading directory metadata root");
        return boxVolume.getDirectoryMetadata();
    }

    @Override
    public void reload() throws QblStorageException {
        dm = reloadMetadata();
    }
}
