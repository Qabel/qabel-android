package de.qabel.qabelbox.storage;

import android.content.Context;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.crypto.DecryptedPlaintext;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.exceptions.QblStorageNotFound;
import de.qabel.qabelbox.providers.BoxProvider;
import de.qabel.qabelbox.providers.DocumentIdParser;
import de.qabel.qabelbox.storage.navigation.BoxNavigation;
import de.qabel.qabelbox.storage.navigation.FolderNavigation;
import de.qabel.qabelbox.storage.transfer.TransferManager;

public class BoxVolume {

    private static final String TAG = BoxVolume.class.getSimpleName();
    private static final String PATH_ROOT = "/";
    private final String rootId;
    private final Context context;
    private final BoxManager boxManager;

    private QblECKeyPair keyPair;
    private byte[] deviceId;
    private CryptoUtils cryptoUtils;
    private File tempDir;
    private String prefix;

    public BoxVolume(
            QblECKeyPair keyPair, String prefix,
            byte[] deviceId, Context context, BoxManager boxManager) {

        this.keyPair = keyPair;
        this.deviceId = deviceId;
        this.context = context;
        cryptoUtils = new CryptoUtils();
        tempDir = context.getCacheDir();

        this.rootId = new DocumentIdParser().buildId(
                keyPair.getPub().getReadableKeyIdentifier(), prefix, null);
        this.prefix = prefix;
        this.boxManager = boxManager;
    }

    public String getDocumentId(String path) {
        return rootId + BoxProvider.DOCID_SEPARATOR + path;
    }

    public BoxNavigation navigate() throws QblStorageException {
        return new FolderNavigation(prefix, getDirectoryMetadata(), keyPair, null, deviceId, boxManager,
                this, PATH_ROOT, null, context);
    }

    public DirectoryMetadata getDirectoryMetadata() throws QblStorageException {

        String rootRef = getRootRef();
        Log.d(TAG, "Downloading Root " + rootRef);
        File indexDl = boxManager.blockingDownload(prefix, rootRef, null);
        File tmp;
        try {
            byte[] encrypted = IOUtils.toByteArray(new FileInputStream(indexDl));
            if (encrypted.length == 0) {
                throw new QblStorageException("Empty file");
            }
            DecryptedPlaintext plaintext = cryptoUtils.readBox(keyPair, encrypted);
            // Should work fine for the small metafiles
            tmp = File.createTempFile("dir", "db", tempDir);
            OutputStream out = new FileOutputStream(tmp);
            out.write(plaintext.getPlaintext());
            out.close();
        } catch (IOException | InvalidCipherTextException | InvalidKeyException e) {
            throw new QblStorageException(e);
        }
        return DirectoryMetadata.openDatabase(tmp, deviceId, rootRef, tempDir);
    }

    public String getRootRef() throws QblStorageException {

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new QblStorageException(e);
        }
        md.update(this.prefix.getBytes());
        md.update(keyPair.getPrivateKey());
        byte[] digest = md.digest();
        byte[] firstBytes = Arrays.copyOfRange(digest, 0, 16);
        ByteBuffer bb = ByteBuffer.wrap(firstBytes);
        UUID uuid = new UUID(bb.getLong(), bb.getLong());
        return uuid.toString();
    }

    public String getPublicKeyIdentifier(){
        return this.keyPair.getPub().getReadableKeyIdentifier();
    }

    public void createIndex() throws QblStorageException {
        String rootRef = getRootRef();
        Log.d(TAG, "Uploading Root " + rootRef);
        DirectoryMetadata dm = DirectoryMetadata.newDatabase(rootRef, deviceId, tempDir);
        try {
            byte[] plaintext = IOUtils.toByteArray(new FileInputStream(dm.path));
            byte[] encrypted = cryptoUtils.createBox(keyPair, keyPair.getPub(), plaintext, 0);
            boxManager.blockingUpload(prefix, rootRef, new ByteArrayInputStream(encrypted));
        } catch (IOException e) {
            throw new QblStorageException(e);
        } catch (InvalidKeyException e) {
            throw new QblStorageException(e);
        }
    }
}
