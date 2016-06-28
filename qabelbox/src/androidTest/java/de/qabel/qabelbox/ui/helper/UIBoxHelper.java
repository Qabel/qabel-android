package de.qabel.qabelbox.ui.helper;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.DropServer;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.drop.AdjustableDropIdGenerator;
import de.qabel.core.drop.DropIdGenerator;
import de.qabel.core.drop.DropURL;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.helper.RealTokerGetter;
import de.qabel.qabelbox.providers.BoxProvider;
import de.qabel.qabelbox.storage.navigation.BoxNavigation;
import de.qabel.qabelbox.util.BoxTestHelper;

public class UIBoxHelper {

    private final String TAG = this.getClass().getSimpleName();

    private Map<String, BoxVolume> identityVolumes = new HashMap<>();

    @Deprecated
    public BoxVolume mBoxVolume;

    private Context context;
    private BoxManager boxManager;

    private IdentityRepository identityRepository;
    private AppPreference preference;
    private ContactRepository contactRepository;

    public UIBoxHelper(Context context) {
        this.context = context;
        BoxTestHelper helper = new BoxTestHelper((QabelBoxApplication)context.getApplicationContext());
        boxManager = helper.getBoxManager();
        identityRepository = helper.getIdentityRepository();
        contactRepository = helper.getContactRepository();
        preference = helper.getAppPreferences();
    }

    public void uploadFile(Identity identity, String name, byte[] data, String path) throws QblStorageException, IOException {
        BoxVolume boxVolume = getIdentityVolume(identity);
        uploadFile(boxVolume, name, data, path);
    }

    public void uploadFile(BoxVolume boxVolume, String name, byte[] data, String path) throws IOException {
       try {
           BoxNavigation nav = boxVolume.navigate();
           nav.navigate(path);
           nav.upload(name, new ByteArrayInputStream(data));
           nav.commit();
       }catch (QblStorageException e){
           throw new IOException(e);
       }
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
        BoxFolder folder =  navigation.createFolder(name);
        navigation.commit();
        return folder;
    }

    public Identity addIdentityWithoutVolume(final String identityName) throws Exception {
        Identity identity = createIdentity(identityName);
        identityRepository.save(identity);
        Log.d(TAG, "identity added " + identity.getAlias() + " " + identity.getEcPublicKey().getReadableKeyIdentifier());
        preference.setLastActiveIdentityKey(identity.getKeyIdentifier());
        return identity;
    }

    public Identity addIdentity(final String identityName) throws Exception {

        Identity identity = createIdentity(identityName);
        identityRepository.save(identity);
        Log.d(TAG, "identity added " + identity.getAlias() + " " + identity.getEcPublicKey().getReadableKeyIdentifier());

        preference.setLastActiveIdentityKey(identity.getKeyIdentifier());
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
        BoxVolume boxVolume = boxManager.createBoxVolume(activeIdentity);
        boxVolume.createIndex();
        identityVolumes.put(activeIdentity.getKeyIdentifier(), boxVolume);

        //TODO Legacy
        mBoxVolume = boxVolume;
    }

    public void setActiveIdentity(Identity identity) {
        preference.setLastActiveIdentityKey(identity.getKeyIdentifier());
        mBoxVolume = identityVolumes.get(identity.getKeyIdentifier());
    }

    public void deleteIdentity(Identity identity) throws Exception {
        identityVolumes.remove(identity.getKeyIdentifier());
        identityRepository.delete(identity);
    }

    public Identity getCurrentIdentity() throws Exception {
        String key = preference.getLastActiveIdentityKey();
        if (key != null) {
            return identityRepository.find(key);
        }
        return null;
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
        if (forceCreated && preference.getToken() == null) {
            preference.setToken(new RealTokerGetter().getToken(context));
        } else {
            setTestAccount();
        }
    }

    public void setTestAccount(){
        preference.setToken(TestConstants.TOKEN);
        preference.setAccountName("testUser");
    }

    public void removeAllIdentities()throws Exception {
        Identities identities = identityRepository.findAll();
        for(Identity id : identities.getIdentities()){
            Contacts contacts = contactRepository.find(id);
            for(Contact c : contacts.getContacts()) {
                contactRepository.delete(c, id);
            }
            identityRepository.delete(id);
        }
    }

    public void addContact(Contact contact, Identity identity) throws Exception {
        contactRepository.save(contact, identity);
    }

    public IdentityRepository getIdentityRepository() {
        return identityRepository;
    }

    public ContactRepository getContactRepository() {
        return contactRepository;
    }

    public BoxManager getBoxManager(){
        return this.boxManager;
    }
 }
