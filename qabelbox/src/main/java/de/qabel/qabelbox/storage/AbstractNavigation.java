package de.qabel.qabelbox.storage;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.exceptions.QblServerException;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.exceptions.QblStorageNameConflict;
import de.qabel.qabelbox.exceptions.QblStorageNotFound;
import de.qabel.qabelbox.providers.BoxProvider;

public abstract class AbstractNavigation implements BoxNavigation {

    private static final Logger logger = LoggerFactory.getLogger(AbstractNavigation.class.getName());
    public static final String BLOCKS_PREFIX = "blocks/";
    private static final String TAG = "AbstractNavigation";
    private final FileCache cache;
    private final Context context;
    protected final URLs urls;
    protected byte[] dmKey;

    protected DirectoryMetadata dm;
    protected final QblECKeyPair keyPair;
    protected final byte[] deviceId;
    protected TransferManager transferManager;
    protected final CryptoUtils cryptoUtils;
    protected final String prefix;

    private final BoxVolume boxVolume;
    private final Set<String> deleteQueue = new HashSet<>();
    private final Set<FileUpdate> updatedFiles = new HashSet<>();
    private Stack<BoxFolder> parentBoxFolders;

    protected String currentPath;

    public AbstractNavigation(String prefix, DirectoryMetadata dm, QblECKeyPair keyPair, byte[] dmKey, byte[] deviceId,
                              TransferManager transferManager, BoxVolume boxVolume, String path,
                              @Nullable Stack<BoxFolder> parentBoxFolders, Context context) {
        this.prefix = prefix;
        this.dm = dm;
        this.keyPair = keyPair;
        this.deviceId = deviceId;
        this.transferManager = transferManager;
        this.boxVolume = boxVolume;
        this.currentPath = path;
        this.dmKey = dmKey;
        this.context = context;
        this.cache = new FileCache(context);
        this.urls = new URLs();
        cryptoUtils = new CryptoUtils();
        if (parentBoxFolders != null) {
            this.parentBoxFolders = parentBoxFolders;
        } else {
            this.parentBoxFolders = new Stack<>();
        }
    }

    public String getPath() {
        return currentPath;
    }

    public String getPath(BoxObject object) {
        if (object instanceof BoxFolder) {
            return currentPath + object.name + BoxProvider.PATH_SEP;
        } else {
            return currentPath + object.name;
        }
    }

    private String getParentPath() throws QblStorageException {
        if (currentPath.equals(BoxProvider.PATH_SEP)) {
            throw new QblStorageException("No parent path");
        }
        String pathWithoutTrailingSlash = currentPath.substring(0, currentPath.length() - 1);
        return pathWithoutTrailingSlash.substring(0, pathWithoutTrailingSlash.lastIndexOf(BoxProvider.PATH_SEP) + 1);
    }

    public String getName() {
        String filepath = getPath();
        return filepath.substring(filepath.lastIndexOf('/') + 1, filepath.length());
    }

    protected File blockingDownload(String prefix, String name, BoxTransferListener boxTransferListener) throws QblStorageException {
        File file = transferManager.createTempFile();
        int id = transferManager.download(prefix, name, file, boxTransferListener);
        if (transferManager.waitFor(id)) {
            return file;
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

    protected Long blockingUpload(String prefix, String name,
                                  File file, @Nullable BoxTransferListener boxTransferListener) {
        int id = transferManager.uploadAndDeleteLocalfileOnSuccess(prefix, name, file, boxTransferListener);
        transferManager.waitFor(id);
        return currentSecondsFromEpoch();
    }

    private static long currentSecondsFromEpoch() {
        return System.currentTimeMillis() / 1000;
    }

    @Override
    public boolean hasParent() {
        return parentBoxFolders.size() >= 1;
    }

    @Override
    public void navigateToParent() throws QblStorageException {
        if (hasParent()) {
            BoxFolder parentBoxFolder = parentBoxFolders.pop();
            doNavigate(parentBoxFolder, false);
        } else {
            throw new QblStorageException("No parent folder");
        }
    }

    @Override
    public void navigateToRoot() throws QblStorageException {
        if (hasParent()) {
            while (hasParent()) {
                navigateToParent();
            }
        }
    }

    /**
     * Navigates to a direct subfolder.
     *
     * @param target Subfolder to navigate to
     * @throws QblStorageException
     */
    @Override
    public void navigate(BoxFolder target) throws QblStorageException {
        boolean isSubfolder = false;
        for (BoxFolder boxFolder : listFolders()) {
            if (boxFolder.ref.equals(target.ref)) {
                isSubfolder = true;
                break;
            }
        }
        if (!isSubfolder) {
            throw new QblStorageNotFound(target.name + " is not a direct subfolder of " + currentPath);
        }
        try {
            doNavigate(target, true);
        } catch (QblStorageException e) {
            throw new QblStorageNotFound("Invalid key");
        }
    }

    private void doNavigate(BoxFolder target, boolean isChild) throws QblStorageException {
        // Push current BoxFolder to parentBoxFolders if navigating to a child and set currentPath
        if (isChild) {
            parentBoxFolders.push(new BoxFolder(dm.getFileName(), getName(), dmKey));
            currentPath = currentPath + target.name + BoxProvider.PATH_SEP;
        } else {
            currentPath = getParentPath();
        }
        try {
            // Target is root, using DirectoryMetadata from BoxVolume
            if (target.key == null && target.name.equals("")) {
                dm = boxVolume.getDirectoryMetadata();
                dmKey = null;
            } else {
                File indexDl = blockingDownload(prefix, target.ref, null);
                File tmp = File.createTempFile("dir", "db", dm.getTempDir());
                KeyParameter keyParameter = new KeyParameter(target.key);
                if (cryptoUtils.decryptFileAuthenticatedSymmetricAndValidateTag(
                        new FileInputStream(indexDl), tmp, keyParameter)) {
                    dm = DirectoryMetadata.openDatabase(
                            tmp, deviceId, target.ref, this.dm.getTempDir());
                    dmKey = target.key;
                }
            }
        } catch (IOException | InvalidKeyException e) {
            throw new QblStorageException(e);
        }
    }

    @Override
    public void commit() throws QblStorageException {
        byte[] version = dm.getVersion();
        dm.commit();
        DirectoryMetadata updatedDM = null;
        try {
            updatedDM = reloadMetadata();
        } catch (QblStorageNotFound e) {
            logger.info("Could not reload metadata");
        }
        // the remote version has changed from the _old_ version
        if ((updatedDM != null) && (!Arrays.equals(version, updatedDM.getVersion()))) {
            logger.info("Conflicting version");
            // ignore our local directory metadata
            // all changes that are not inserted in the new dm are _lost_!
            dm = updatedDM;
            for (FileUpdate update : updatedFiles) {
                handleConflict(update);
            }
            dm.commit();
        }
        for (FileUpdate update : updatedFiles) {
            updateFileMetadata(update.updated);
        }
        uploadDirectoryMetadata();
        for (String ref : deleteQueue) {
            blockingDelete(prefix, ref);
        }
        // TODO: make a test fail without these
        deleteQueue.clear();
        updatedFiles.clear();
    }

    protected abstract DirectoryMetadata reloadMetadata() throws QblStorageException;

    protected void blockingDelete(String prefix, String ref) {
        transferManager.delete(prefix, ref);

    }

    private void handleConflict(FileUpdate update) throws QblStorageException {
        BoxFile local = update.updated;
        BoxFile newFile = dm.getFile(local.name);
        if (newFile == null) {
            try {
                dm.insertFile(local);
            } catch (QblStorageNameConflict e) {
                // name clash with a folder or external
                local.name = conflictName(local);
                // try again until we get no name clash
                handleConflict(update);
            }
        } else if (newFile.equals(update.old)) {
            logger.info("No conflict for the file " + local.name);
        } else {
            logger.info("Inserting conflict marked file");
            local.name = conflictName(local);
            if (update.old != null) {
                dm.deleteFile(update.old);
            }
            if (dm.getFile(local.name) == null) {
                dm.insertFile(local);
            }
        }
    }

    private String conflictName(BoxFile local) {
        return local.name + "_conflict_" + local.mtime.toString();
    }

    protected abstract void uploadDirectoryMetadata() throws QblStorageException;

    @Override
    public BoxNavigation navigate(BoxExternalReference target) {
        throw new NotImplementedException("Externals are not yet implemented!");
    }

    @Override
    public List<BoxFile> listFiles() throws QblStorageException {
        return dm.listFiles();
    }

    @Override
    public List<BoxFolder> listFolders() throws QblStorageException {
        return dm.listFolders();
    }

    @Override
    public BoxObject getExternal(String name) throws QblStorageException {
        for (BoxExternalReference boxExternalRefs : dm.listExternalReferences()) {
            if (name.equals(boxExternalRefs.name)) {
                return new BoxObject(name);
            }
        }
        return null;
    }

    @Override
    public List<BoxObject> listExternalNames() throws QblStorageException {
        List<BoxObject> boxExternals = new ArrayList<>();
        for (BoxExternalReference boxExternal : dm.listExternalReferences()) {
            boxExternals.add(new BoxObject(boxExternal.name));
        }
        return boxExternals;
    }

    @Override
    public List<BoxObject> listExternals() throws QblStorageException {
        List<BoxExternalReference> references = dm.listExternalReferences();
        List<BoxObject> boxExternals = new ArrayList<>(references.size());
        List<BoxExternalReference> referencesNotFound = new LinkedList<>();
        for (BoxExternalReference boxExternalRefs : references) {
            try {
                File out = getMetadataFile(boxExternalRefs.getPrefix(), boxExternalRefs.getBlock(), boxExternalRefs.key);
                if (boxExternalRefs.isFolder) {
                    //TODO: Check DirectoryMetadata handling
                    DirectoryMetadata directoryMetadata =
                            DirectoryMetadata.openDatabase(out, dm.deviceId, boxExternalRefs.getBlock(), dm.getTempDir());
                    boxExternals.addAll(directoryMetadata.listFiles());
                    boxExternals.addAll(directoryMetadata.listFolders());
                } else {
                    FileMetadata fileMetadata = new FileMetadata(out);
                    boxExternals.add(fileMetadata.getFile());
                }
            } catch (QblStorageNotFound e) {
                Log.d(TAG, "External reference not found: " + boxExternalRefs.name);
                referencesNotFound.add(boxExternalRefs);
            } catch (QblStorageException e) {
                Log.e(TAG, "Cannot load metadata file: " + boxExternalRefs.getPrefix() + '/' + boxExternalRefs.getBlock());
                if (boxExternalRefs.isFolder) {
                    boxExternals.add(new BoxExternalFolder(boxExternalRefs.url, boxExternalRefs.name,
                            boxExternalRefs.key, false));
                } else {
                    boxExternals.add(new BoxExternalFile(boxExternalRefs.owner, boxExternalRefs.getPrefix(), boxExternalRefs.getBlock(),
                            boxExternalRefs.name, boxExternalRefs.key, false));
                }
            }
        }
        detachExternals(referencesNotFound);

        return boxExternals;
    }

    private void detachExternals(List<BoxExternalReference> references) throws QblStorageException {
        if (references.size() > 0) {
            for (BoxExternalReference reference : references) {
                detachExternal(reference.name);
            }
            commit();
        }
    }

    @NonNull
    private File getMetadataFile(String prefix, String block, byte[] key) throws QblStorageException {
        File encryptedMetadata = blockingDownload(prefix, block, null);

        File out = new File(context.getExternalCacheDir(), UUID.randomUUID().toString());

        try (InputStream decryptedInputStream = openStream(key, encryptedMetadata);
             FileOutputStream fileOutputStream = new FileOutputStream(out)) {
            IOUtils.copy(decryptedInputStream, fileOutputStream);
        } catch (IOException e) {
            throw new QblStorageException("Could not decrypt FileMetadata", e);
        }
        return out;
    }

    @Override
    public BoxFile upload(String name, InputStream content,
                          @Nullable BoxTransferListener boxTransferListener) throws QblStorageException {
        KeyParameter key = cryptoUtils.generateSymmetricKey();
        String block = UUID.randomUUID().toString();
        BoxFile boxFile = new BoxFile(prefix, block, name, null, 0L, key.getKey());
        SimpleEntry<Long, Long> mtimeAndSize = uploadEncrypted(content, key, prefix, BLOCKS_PREFIX + block, boxTransferListener);
        boxFile.mtime = mtimeAndSize.getKey();
        boxFile.size = mtimeAndSize.getValue();
        // Overwrite = delete old file, uploadAndDeleteLocalfile new file
        BoxFile oldFile = dm.getFile(name);
        if (oldFile != null) {
            if (oldFile.meta != null && oldFile.metakey != null) {
                boxFile.meta = oldFile.meta;
                boxFile.metakey = oldFile.metakey.clone();
            }
            deleteQueue.add(oldFile.block);
            dm.deleteFile(oldFile);
        }
        updatedFiles.add(new FileUpdate(oldFile, boxFile));
        dm.insertFile(boxFile);
        return boxFile;
    }

    protected SimpleEntry<Long, Long> uploadEncrypted(
            InputStream content, KeyParameter key, String prefix, String block,
            @Nullable BoxTransferListener boxTransferListener) throws QblStorageException {
        try {
            File tempFile = File.createTempFile("uploadAndDeleteLocalfile", "up", dm.getTempDir());
            OutputStream outputStream = new FileOutputStream(tempFile);
            if (!cryptoUtils.encryptStreamAuthenticatedSymmetric(content, outputStream, key, null)) {
                throw new QblStorageException("Encryption failed");
            }
            outputStream.flush();
            Long size = tempFile.length();
            Long mtime = blockingUpload(prefix, block, tempFile, boxTransferListener);
            return new SimpleEntry<>(mtime, size);
        } catch (IOException | InvalidKeyException e) {
            throw new QblStorageException(e);
        }
    }

    @Override
    public InputStream download(BoxFile boxFile, @Nullable BoxTransferListener boxTransferListener) throws QblStorageException {
        File download = cache.get(boxFile);
        cache.close();
        if (download == null) {
            download = refreshCache(boxFile, boxTransferListener);
        }
        try {
            return openStream(boxFile.key, download);
        } catch (QblStorageException e) {
            download = refreshCache(boxFile, boxTransferListener);
            return openStream(boxFile.key, download);
        }
    }

    /**
     * Creates and uploads a FileMetadata object for a BoxFile. FileMetadata location is written to BoxFile.meta
     * and encryption key to BoxFile.metakey. If BoxFile.meta or BoxFile.metakey is not null, BoxFile will not be
     * modified and no FileMetadata will be created. Call {@link #removeFileMetadata(BoxFile boxFile)}
     * first if you want to re-create a new FileMetadata.
     *
     * @param boxFile BoxFile to create FileMetadata from.
     * @return True if FileMetadata has successfully created and uploaded.
     */
    @Override
    public BoxExternalReference createFileMetadata(QblECPublicKey owner, BoxFile boxFile) throws QblStorageException {

        if (boxFile.meta != null || boxFile.metakey != null) {
            return new BoxExternalReference(false,
                    urls.getFiles() + boxFile.prefix + '/' + boxFile.meta, boxFile.name, owner, boxFile.metakey);
        }
        String metaBlock = UUID.randomUUID().toString();
        KeyParameter key = cryptoUtils.generateSymmetricKey();
        boxFile.meta = metaBlock;
        boxFile.metakey = key.getKey();

        try {
            FileMetadata fileMetadata = new FileMetadata(owner, boxFile, dm.getTempDir());
            FileInputStream fileInputStream = new FileInputStream(fileMetadata.getPath());
            uploadEncrypted(fileInputStream, key, prefix, metaBlock, null);

            // Overwrite = delete old file, uploadAndDeleteLocalfile new file
            BoxFile oldFile = dm.getFile(boxFile.name);
            if (oldFile != null) {
                dm.deleteFile(oldFile);
            }
            dm.insertFile(boxFile);
            reloadMetadata();
        } catch (QblStorageException | FileNotFoundException e) {
            throw new QblStorageException("Could not create or upload FileMetadata", e);
        }
        return new BoxExternalReference(false,
                urls.getFiles() + boxFile.prefix + '/' + metaBlock, boxFile.name, owner, boxFile.metakey);

    }

    /**
     * Updates and uploads a FileMetadata object for a BoxFile.
     *
     * @param boxFile BoxFile to create FileMetadata from.
     * @return True if FileMetadata has successfully created and uploaded.
     */
    @Override
    public boolean updateFileMetadata(BoxFile boxFile) {
        if (boxFile.meta == null || boxFile.metakey == null) {
            return false;
        }
        try {
            File out = getMetadataFile(boxFile.prefix, boxFile.meta, boxFile.metakey);
            FileMetadata fileMetadataOld = new FileMetadata(out);
            FileMetadata fileMetadataNew = new FileMetadata(fileMetadataOld.getFile().owner, boxFile, dm.getTempDir());
            FileInputStream fileInputStream = new FileInputStream(fileMetadataNew.getPath());
            uploadEncrypted(fileInputStream, new KeyParameter(boxFile.metakey), boxFile.prefix, boxFile.meta, null);
        } catch (QblStorageException | FileNotFoundException e) {
            Log.e(TAG, "Could not create or uploadAndDeleteLocalfile FileMetadata", e);
            return false;
        }
        return true;
    }

    /**
     * Deletes FileMetadata and sets BoxFile.meta and BoxFile.metakey to null. Does not re-encrypt BoxFile thus
     * receivers of the FileMetadata can still read the BoxFile.
     *
     * @param boxFile BoxFile to remove FileMetadata from.
     * @return True if FileMetadata has been deleted. False if meta information is missing.
     */
    @Override
    public boolean removeFileMetadata(BoxFile boxFile) {
        if (boxFile.meta == null || boxFile.metakey == null) {
            return false;
        }

        blockingDelete(boxFile.prefix, boxFile.meta);
        boxFile.meta = null;
        boxFile.metakey = null;

        try {
            // Overwrite = delete old file, upload new file
            BoxFile oldFile = dm.getFile(boxFile.name);
            if (oldFile != null) {
                dm.deleteFile(oldFile);
            }
            dm.insertFile(boxFile);
            reloadMetadata();
        } catch (QblStorageException e) {
            Log.e(TAG, "error until reload metadata", e);
        }
        return true;
    }

    /**
     * Attaches a received BoxExternal to the DirectoryMetadata
     *
     * @param boxExternalReference Reference to meta file
     * @throws QblStorageException If metadata cannot be accesses or decrypted.
     */
    @Override
    public void attachExternal(BoxExternalReference boxExternalReference) throws QblStorageException {
        File out = getMetadataFile(boxExternalReference.getPrefix(),
                boxExternalReference.getBlock(), boxExternalReference.key);
        FileMetadata fileMetadata = new FileMetadata(out);
        BoxExternalFile file = fileMetadata.getFile();
        boxExternalReference.name = file.name;
        dm.insertExternalReference(boxExternalReference);
    }

    /**
     * Deletes a BoxExternalFile from the DirectoryMetadata.
     *
     * @param name Name of received shared BoxFile to delete from DirectoryMetadata.
     * @throws QblStorageException
     */
    @Override
    public void detachExternal(String name) throws QblStorageException {
        dm.deleteExternalReference(name);
    }

    private File refreshCache(BoxFile boxFile, @Nullable BoxTransferListener boxTransferListener) throws QblStorageException {
        logger.info("Refreshing cache: " + boxFile.block);
        File download = blockingDownload(boxFile.prefix, BLOCKS_PREFIX + boxFile.block, boxTransferListener);
        cache.put(boxFile, download);
        cache.close();
        return download;
    }

    @NonNull
    private InputStream openStream(byte[] boxFileKey, File file) throws QblStorageException {
        KeyParameter key = new KeyParameter(boxFileKey);
        try {
            File temp = File.createTempFile("uploadAndDeleteLocalfile", "down", dm.getTempDir());
            if (!cryptoUtils.decryptFileAuthenticatedSymmetricAndValidateTag(
                    new FileInputStream(file), temp, key)
                    || checkFile(temp)) {
                throw new QblStorageException("Decryption failed");
            }
            return new FileInputStream(temp);
        } catch (IOException | InvalidKeyException e) {
            throw new QblStorageException(e);
        }
    }

    private boolean checkFile(File file) {
        // because the decrypt method does not raise an exception if it fails.
        return file.length() == 0;
    }

    @Override
    public BoxFolder createFolder(String name) throws QblStorageException {
        DirectoryMetadata dm = DirectoryMetadata.newDatabase(null, deviceId, this.dm.getTempDir());
        KeyParameter secretKey = cryptoUtils.generateSymmetricKey();
        BoxFolder folder = new BoxFolder(dm.getFileName(), name, secretKey.getKey());
        this.dm.insertFolder(folder);
        BoxNavigation newFolder = new FolderNavigation(prefix, dm, keyPair, secretKey.getKey(),
                deviceId, transferManager, boxVolume, currentPath + BoxProvider.PATH_SEP + folder.name,
                parentBoxFolders, context);
        newFolder.commit();
        return folder;
    }

    @Override
    public void delete(BoxObject boxObject) throws QblStorageException {
        logger.info("Deleting object: " + boxObject.name);
        if (boxObject instanceof BoxFile) {
            delete((BoxFile) boxObject);
        } else if (boxObject instanceof BoxFolder) {
            delete((BoxFolder) boxObject);
        }
    }

    @Override
    public void delete(BoxFile file) throws QblStorageException {
        dm.deleteFile(file);
        cache.remove(file);
        cache.close();
        deleteQueue.add(BLOCKS_PREFIX + file.block);
    }

    @Override
    public void delete(BoxFolder folder) throws QblStorageException {
        navigate(folder);
        for (BoxFile file : listFiles()) {
            logger.info("Deleting file " + file.name);
            delete(file);
        }
        for (BoxFolder subFolder : listFolders()) {
            logger.info("Deleting folder " + folder.name);
            delete(subFolder);
        }
        navigateToParent();
        commit();
        dm.deleteFolder(folder);
        deleteQueue.add(folder.ref);
    }

    @Override
    public void delete(BoxExternalReference external) throws QblStorageException {
        throw new NotImplementedException("Externals are not yet implemented!");
    }

    @Override
    public BoxFile rename(BoxFile file, String name) throws QblStorageException {
        dm.deleteFile(file);
        file.name = name;
        dm.insertFile(file);
        updateFileMetadata(file);
        return file;
    }

    @Override
    public BoxFolder rename(BoxFolder folder, String name) throws QblStorageException {
        dm.deleteFolder(folder);
        folder.name = name;
        dm.insertFolder(folder);
        return folder;
    }

    @Override
    public BoxExternalReference rename(BoxExternalReference external, String name) throws QblStorageException {
        dm.deleteExternalReference(external.name);
        external.name = name;
        dm.insertExternalReference(external);
        return external;
    }

    private static class FileUpdate {
        final BoxFile old;
        final BoxFile updated;

        public FileUpdate(BoxFile old, BoxFile updated) {
            this.old = old;
            this.updated = updated;
        }

        @Override
        public int hashCode() {
            int result = old != null ? old.hashCode() : 0;
            result = 31 * result + (updated != null ? updated.hashCode() : 0);
            return result;
        }
    }
}
