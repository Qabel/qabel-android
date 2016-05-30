package de.qabel.qabelbox.ui.helper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.qabel.core.config.Contact;
import de.qabel.core.config.DropServer;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.drop.AdjustableDropIdGenerator;
import de.qabel.core.drop.DropIdGenerator;
import de.qabel.core.drop.DropURL;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.helper.RealTokerGetter;
import de.qabel.qabelbox.providers.BoxProvider;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.storage.BoxVolume;
import de.qabel.qabelbox.storage.StorageSearch;
import de.qabel.qabelbox.storage.model.BoxFile;
import de.qabel.qabelbox.storage.model.BoxFolder;
import de.qabel.qabelbox.storage.model.BoxObject;
import de.qabel.qabelbox.storage.navigation.BoxNavigation;

public class UIBoxHelper {

    private final String TAG = this.getClass().getSimpleName();

    private LocalQabelService mService;
    private Map<String, BoxVolume> identityVolumes = new HashMap<>();

    private boolean finished = false;

    @Deprecated
    public BoxVolume mBoxVolume;

    public UIBoxHelper(Context context) {

    }

    public UIBoxHelper() {
    }

    public void unbindService(final QabelBoxApplication app) {

        Intent serviceIntent = new Intent(app.getApplicationContext(), LocalQabelService.class);
        finished = false;
        app.stopService(serviceIntent);
    }

    public void bindService(final QabelBoxApplication app) {

        Intent serviceIntent = new Intent(app.getApplicationContext(), LocalQabelService.class);
        finished = false;
        //app.stopService(serviceIntent);
        app.bindService(serviceIntent, new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

                Log.d(TAG, "LocalQabelService connected");
                LocalQabelService.LocalBinder binder = (LocalQabelService.LocalBinder) service;
                mService = binder.getService();

                finished = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

                mService = null;
            }
        }, Context.BIND_AUTO_CREATE);
        while (!finished) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void uploadFile(Identity identity, String name, byte[] data, String path) throws QblStorageException, IOException {
        BoxVolume boxVolume = getIdentityVolume(identity);
        uploadFile(boxVolume, name, data, path);
    }

    public void uploadFile(BoxVolume boxVolume, String name, byte[] data, String path) throws IOException {
        String folderId = boxVolume.getDocumentId(path);
        Uri uploadUri = DocumentsContract.buildDocumentUri(
                BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, folderId + name);
        Context self = QabelBoxApplication.getInstance().getApplicationContext();

        OutputStream upload = self.getContentResolver().openOutputStream(uploadUri, "w");
        upload.write(data);
        upload.close();
    }

    private <S extends BoxObject> S getBoxObject(Class<S> boxClazz, BoxNavigation boxNavigation, String name) throws QblStorageException {

        List<S> objects = null;
        if (boxClazz == BoxFile.class) {
            objects = (List<S>) boxNavigation.listFiles();
        } else if (boxClazz == BoxFolder.class) {
            objects = (List<S>) boxNavigation.listFolders();
        }

        for (S boxObject : objects) {
            if (boxObject.name.equals(name)) {
                return boxObject;
            }
        }
        return null;
    }

    private void navigateToPath(BoxNavigation boxNavigation, String path) throws QblStorageException {
        if (boxNavigation.hasParent()) {
            boxNavigation.navigateToRoot();
        }
        if (path != null) {
            String[] parts = path.split(BoxProvider.PATH_SEP);
            if (parts.length > 0) {
                for (String part : parts) {
                    BoxFolder current = getBoxObject(BoxFolder.class, boxNavigation, part);
                    if (current != null) {
                        boxNavigation.navigate(current);
                    }
                }
                if (!boxNavigation.getPath().equals(path)) {
                    throw new QblStorageException("Cannot navigate to path: " + path);
                }
            }
        }
    }

    public BoxVolume getIdentityVolume(Identity identity) throws QblStorageException {
        BoxVolume boxVolume = identityVolumes.get(identity.getKeyIdentifier());
        if (boxVolume == null) {
            throw new QblStorageException("Volume for identity not initialized!");
        }
        return boxVolume;
    }

    private BoxNavigation createNavigation(Identity identity, String path) throws QblStorageException {
        BoxVolume boxVolume = getIdentityVolume(identity);
        BoxNavigation navigation = boxVolume.navigate();
        navigateToPath(navigation, path);
        return navigation;
    }

    public BoxFolder createFolder(String name, Identity identity, String path) throws QblStorageException {
        BoxNavigation navigation = createNavigation(identity, path);
        return navigation.createFolder(name);
    }

    public Identity addIdentityWithoutVolume(final String identityName) {

        Identity identity = createIdentity(identityName);
        mService.addIdentity(identity);
        Log.d(TAG, "identity added " + identity.getAlias() + " " + identity.getEcPublicKey().getReadableKeyIdentifier());
        mService.setActiveIdentity(identity);
        return identity;
    }

    public Identity addIdentity(final String identityName) {

        Identity identity = createIdentity(identityName);
        mService.addIdentity(identity);
        Log.d(TAG, "identity added " + identity.getAlias() + " " + identity.getEcPublicKey().getReadableKeyIdentifier());
        mService.setActiveIdentity(identity);

        try {
            initBoxVolume(identity);
        } catch (QblStorageException e) {
            Log.e(TAG, "Cannot initialize BoxVolume for Identity " + identity.getAlias(), e);
        }

        return identity;
    }

    @NonNull
    public Identity createIdentity(String identityName) {
        Collection<DropURL> dropURLs = createDropURLList();
        Identity identity = new Identity(identityName, dropURLs, new QblECKeyPair());
        identity.getPrefixes().add(TestConstants.PREFIX);
        return identity;
    }

    @NonNull
    public Collection<DropURL> createDropURLList() {
        URI uri = URI.create(QabelBoxApplication.DEFAULT_DROP_SERVER);
        DropServer dropServer = new DropServer(uri, "", true);
        DropIdGenerator adjustableDropIdGenerator = new AdjustableDropIdGenerator(2 * 8);
        DropURL dropURL = new DropURL(dropServer, adjustableDropIdGenerator);
        Collection<DropURL> dropURLs = new ArrayList<>();
        dropURLs.add(dropURL);
        return dropURLs;
    }

    @NonNull
    public Contact createContact(String contactName) {
        DropIdGenerator adjustableDropIdGenerator = new AdjustableDropIdGenerator(2 * 8);
        return new Contact(contactName, createDropURLList(), new QblECKeyPair().getPub());
    }

    private void initBoxVolume(Identity activeIdentity) throws QblStorageException {
/*        BoxVolume boxVolume = provider.getVolumeForRoot(
                activeIdentity.getEcPublicKey().getReadableKeyIdentifier(),
                VolumeFileTransferHelper.getPrefixFromIdentity(activeIdentity));
        boxVolume.createIndex();
        identityVolumes.put(activeIdentity.getKeyIdentifier(), boxVolume);

        //TODO Legacy
        mBoxVolume = boxVolume;*/
    }

    public void setActiveIdentity(Identity identity) {
        mService.setActiveIdentity(identity);
        mBoxVolume = identityVolumes.get(identity.getKeyIdentifier());
    }

    public void deleteIdentity(Identity identity) {
        identityVolumes.remove(identity.getKeyIdentifier());
        mService.deleteIdentity(identity);
    }

    public Identity getCurrentIdentity() {
        return mService.getActiveIdentity();
    }

    /**
     * wait until the volume contain a defined count of files
     *
     * @param fileCount
     */
    public void waitUntilFileCount(Identity identity, int fileCount) throws QblStorageException {
        waitUntilFileCount(getIdentityVolume(identity), fileCount);
    }

    public void waitUntilFileCount(BoxVolume volume, int fileCount) {

        try {
            BoxNavigation boxNavigation = volume.navigate();
            StorageSearch storageSearch = new StorageSearch(boxNavigation);
            while (storageSearch.getResultSize() < fileCount) {
                Log.d(TAG, "wait until all files uploaded " + storageSearch.getResultSize() + "/" + fileCount);
                Thread.sleep(200);
                boxNavigation.reload();
                storageSearch.refreshRange(boxNavigation, true);
            }
        } catch (QblStorageException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * create new token if needed
     *
     * @param forceCreated set to true if create new forced
     */
    public void createTokenIfNeeded(boolean forceCreated) {
        Context applicationContext = QabelBoxApplication.getInstance().getApplicationContext();
        AppPreference prefs = new AppPreference(applicationContext);
        if (forceCreated && prefs.getToken() == null) {
            prefs.setToken(new RealTokerGetter().getToken(applicationContext));
        } else {
            prefs.setToken(TestConstants.TOKEN);
            prefs.setAccountName("testUser");
        }
    }

    public void removeAllIdentities() {
        mService.deleteContactsAndIdentities();
    }

    public LocalQabelService getService() {
        return mService;
    }

}
