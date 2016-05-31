package de.qabel.qabelbox.storage.navigation;

import android.content.Context;
import android.support.annotation.Nullable;

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

    private static final Logger logger = LoggerFactory.getLogger(FolderNavigation.class.getName());

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
        try {
            byte[] plaintext = FileHelper.toByteArray(new FileInputStream(dm.path));
            byte[] encrypted = boxManager.getCryptoUtils().
                    createBox(keyPair, keyPair.getPub(), plaintext, 0);
            boxManager.blockingUpload(prefix, dm.getFileName(), new ByteArrayInputStream(encrypted));
        } catch (IOException | InvalidKeyException e) {
            throw new QblStorageException(e);
        }
    }

    private void uploadDirectoryMetadataSubFolder() throws QblStorageException {
        logger.info("Uploading directory metadata");
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
        logger.info("Reloading directory metadata");
        // duplicate of navigate()
        File indexDl = boxManager.downloadDecrypted(prefix, dm.getFileName(), dmKey, null);
        return DirectoryMetadata.openDatabase(indexDl, deviceId, dm.getFileName(), dm.getTempDir());
    }

    protected DirectoryMetadata reloadMetadataRoot() throws QblStorageException {
        // TODO: duplicate with BoxVoume.navigate()
        String rootRef = dm.getFileName();
        File indexDl = boxManager.blockingDownload(prefix, rootRef, null);
        File tmp;
        try {
            byte[] encrypted = FileHelper.toByteArray(new FileInputStream(indexDl));
            DecryptedPlaintext plaintext = boxManager.getCryptoUtils().readBox(keyPair, encrypted);
            tmp = File.createTempFile("dir", "db", dm.getTempDir());
            logger.info("Using " + tmp.toString() + " for the metadata file");
            OutputStream out = new FileOutputStream(tmp);
            out.write(plaintext.getPlaintext());
            out.close();
        } catch (IOException | InvalidCipherTextException | InvalidKeyException e) {
            throw new QblStorageException(e);
        }
        return DirectoryMetadata.openDatabase(tmp, deviceId, rootRef, dm.getTempDir());
    }

    @Override
    public void reload() throws QblStorageException {
        dm = reloadMetadata();
    }
}
