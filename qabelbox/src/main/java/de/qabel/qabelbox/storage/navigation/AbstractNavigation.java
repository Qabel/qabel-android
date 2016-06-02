package de.qabel.qabelbox.storage.navigation;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.desktop.StringUtils;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.exceptions.QblStorageNameConflict;
import de.qabel.qabelbox.exceptions.QblStorageNotFound;
import de.qabel.qabelbox.providers.BoxProvider;
import de.qabel.qabelbox.providers.DocumentId;
import de.qabel.qabelbox.storage.BoxManager;
import de.qabel.qabelbox.storage.BoxVolume;
import de.qabel.qabelbox.storage.DirectoryMetadata;
import de.qabel.qabelbox.storage.FileMetadata;
import de.qabel.qabelbox.storage.model.BoxExternalFile;
import de.qabel.qabelbox.storage.model.BoxExternalFolder;
import de.qabel.qabelbox.storage.model.BoxExternalReference;
import de.qabel.qabelbox.storage.model.BoxFile;
import de.qabel.qabelbox.storage.model.BoxFolder;
import de.qabel.qabelbox.storage.model.BoxObject;

public abstract class AbstractNavigation implements BoxNavigation {

    private static final Logger logger = LoggerFactory.getLogger(AbstractNavigation.class.getName());
    public static final String BLOCKS_PREFIX = "blocks/";
    private static final String TAG = "AbstractNavigation";
    private final Context context;
    protected byte[] dmKey;

    protected DirectoryMetadata dm;
    protected final QblECKeyPair keyPair;
    protected final byte[] deviceId;

    protected final String prefix;

    protected final BoxVolume boxVolume;
    protected final BoxManager boxManager;

    private final Set<String> deleteQueue = new HashSet<>();
    private final Set<FileUpdate> updatedFiles = new HashSet<>();
    private Stack<BoxFolder> parentBoxFolders;

    protected String currentPath;

    public AbstractNavigation(String prefix, DirectoryMetadata dm, QblECKeyPair keyPair, byte[] dmKey, byte[] deviceId,
                              BoxManager boxManager, BoxVolume boxVolume, String path,
                              @Nullable Stack<BoxFolder> parentBoxFolders, Context context) {
        this.prefix = prefix;
        this.dm = dm;
        this.keyPair = keyPair;
        this.deviceId = deviceId;
        this.boxVolume = boxVolume;
        this.currentPath = path;
        this.dmKey = dmKey;
        this.context = context;
        this.boxManager = boxManager;
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
     * @param target Subfolder to navigateToChild to
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
            e.printStackTrace();
            throw new QblStorageNotFound("Invalid key");
        }
    }

    @Override
    public BoxFile getFile(String name, boolean forceNotNull) throws QblStorageException {
        for (BoxFile file : listFiles()) {
            if (file.name.equals(name)) {
                return file;
            }
        }
        for (BoxObject boxObject : listExternals()) {
            if (boxObject.name.equals(name) && boxObject instanceof BoxExternalFile) {
                return (BoxFile) boxObject;
            }
        }
        if(forceNotNull){
            throw new QblStorageNotFound(name + " not found.");
        }
        return null;
    }

    @Override
    public void navigate(String path) throws QblStorageException {
        if(parentBoxFolders.size() > 0){
            navigateToRoot();
        }
        String[] parts = path.split(BoxProvider.PATH_SEP);
        for (String current : parts) {
            if (current.isEmpty()) {
                continue;
            }
            if (!navigateToChild(current)) {
                throw new QblStorageNotFound("Cannot navigateToChild to: " + current + "(" + path + ")");
            }
        }
    }

    private boolean navigateToChild(String target) throws QblStorageException {
        for (BoxFolder boxFolder : listFolders()) {
            if (boxFolder.name.equals(target)) {
                doNavigate(boxFolder, true);
                return true;
            }
        }
        for (BoxFile f : listFiles()) {
            if (f.name.equals(target)) {
                return true;
            }
        }
        for (BoxObject f : listExternals()) {
            if (f.name.equals(target)) {
                return true;
            }
        }
        return false;
    }

    private void doNavigate(BoxFolder target, boolean isChild) throws QblStorageException {
        // Push current BoxFolder to parentBoxFolders if navigating to a child and set currentPath
        if (isChild) {
            parentBoxFolders.push(new BoxFolder(dm.getFileName(), getName(), dmKey));
            currentPath = currentPath + target.name + BoxProvider.PATH_SEP;
        } else {
            currentPath = getParentPath();
        }

        // Target is root, using DirectoryMetadata from BoxVolume
        if (target.key == null && target.name.equals("")) {
            dm = boxVolume.getDirectoryMetadata();
            dmKey = null;
        } else {
            File indexDl = boxManager.downloadDecrypted(prefix, target.ref, target.key, null);
            dm = DirectoryMetadata.openDatabase(
                    indexDl, deviceId, target.ref, this.dm.getTempDir());
            dmKey = target.key;
        }
    }

    @Override
    public void commit() throws QblStorageException {
        byte[] version = dm.getVersion();
        dm.commit();
        Log.d(TAG, "Committing DM (" + dm.getFileName() +")" + getPath());
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
            boxManager.delete(prefix, ref);
        }
        // TODO: make a test fail without these
        deleteQueue.clear();
        updatedFiles.clear();
    }

    protected abstract DirectoryMetadata reloadMetadata() throws QblStorageException;

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
        return boxManager.downloadDecrypted(prefix, block, key, null);
    }

    @Override
    public BoxFile upload(String name, InputStream content) throws QblStorageException {

        DocumentId documentId = new DocumentId(boxVolume.getPublicKeyIdentifier(),
                prefix, this.getPath().split(BoxProvider.PATH_SEP), name);

        BoxFile resultFile = boxManager.uploadEncrypted(documentId.toString(), content);

        BoxFile oldFile = dm.getFile(name);
        if (oldFile != null) {
            if (oldFile.meta != null && oldFile.metakey != null) {
                resultFile.meta = oldFile.meta;
                resultFile.metakey = oldFile.metakey.clone();
            }
            deleteQueue.add(oldFile.block);
            dm.deleteFile(oldFile);
        }
        updatedFiles.add(new FileUpdate(oldFile, resultFile));
        dm.insertFile(resultFile);
        return resultFile;
    }

    @Override
    public InputStream download(BoxFile boxFile) throws QblStorageException {
        return boxManager.downloadStreamDecrypted(boxFile, boxVolume.getPublicKeyIdentifier(), getPath());
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
                    createFileMetaURL(boxFile), boxFile.name, owner, boxFile.metakey);
        }
        try {
            FileMetadata fileMetadata = new FileMetadata(owner, boxFile, dm.getTempDir());
            FileInputStream fileInputStream = new FileInputStream(fileMetadata.getPath());

            String metaBlock = UUID.randomUUID().toString();
            byte[] key = boxManager.getCryptoUtils().generateSymmetricKey().getKey();

            boxManager.uploadEncrypted(prefix, metaBlock, key, fileInputStream, null);

            boxFile.meta = metaBlock;
            boxFile.metakey = key;

            // Overwrite = delete old file, uploadAndDeleteLocalfile new file
            BoxFile oldFile = dm.getFile(boxFile.name);
            if (oldFile != null) {
                dm.deleteFile(oldFile);
            }
            dm.insertFile(boxFile);
            reloadMetadata();

            return new BoxExternalReference(false, createFileMetaURL(boxFile), boxFile.name, owner, boxFile.metakey);
        } catch (QblStorageException | FileNotFoundException e) {
            throw new QblStorageException("Could not create or uploadEncrypted FileMetadata", e);
        }
    }

    private String createFileMetaURL(BoxFile boxFile) {
        return new URLs(context).getFiles()
                + boxFile.prefix + BoxProvider.PATH_SEP + boxFile.meta;
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
            boxManager.uploadEncrypted(boxFile.prefix, boxFile.meta, boxFile.metakey, fileInputStream, null);
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

        try {
            boxManager.delete(boxFile.prefix, boxFile.meta);
            boxFile.meta = null;
            boxFile.metakey = null;

            // Overwrite = delete old file, uploadEncrypted new file
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

    @Override
    public BoxFolder createFolder(String name) throws QblStorageException {
        DirectoryMetadata dm = DirectoryMetadata.newDatabase(null, deviceId, this.dm.getTempDir());
        byte[] secretKey = boxManager.getCryptoUtils().generateSymmetricKey().getKey();
        BoxFolder folder = new BoxFolder(dm.getFileName(), name, secretKey);
        this.dm.insertFolder(folder);
        BoxNavigation newFolder = new FolderNavigation(prefix, dm, keyPair, secretKey,
                deviceId, boxManager, boxVolume, currentPath + folder.name + BoxProvider.PATH_SEP,
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
        boxManager.delete(file.prefix, file.block);
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
