package de.qabel.qabelbox.ui.helper;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

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
import de.qabel.qabelbox.util.BoxTestHelper;

public class UIBoxHelper {

    private final String TAG = this.getClass().getSimpleName();

    private Context context;

    private IdentityRepository identityRepository;
    private AppPreference preference;
    private ContactRepository contactRepository;

    public UIBoxHelper(Context context) {
        this.context = context;
        BoxTestHelper helper = new BoxTestHelper((QabelBoxApplication)context.getApplicationContext());
        identityRepository = helper.getIdentityRepository();
        contactRepository = helper.getContactRepository();
        preference = helper.getAppPreferences();
    }

    public Identity addIdentityWithoutVolume(final String identityName) throws Exception {
        Identity identity = createIdentity(identityName);
        identityRepository.save(identity);
        Log.d(TAG, "identity added " + identity.getAlias() + " " + identity.getEcPublicKey().getReadableKeyIdentifier());
        preference.setLastActiveIdentityKey(identity.getKeyIdentifier());
        return identity;
    }

    public Identity addIdentity(final String identityName) throws Exception {

        return addIdentityWithoutVolume(identityName);
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
        return new Contact(contactName, createDropURLList(), new QblECKeyPair().getPub());
    }

    public void deleteIdentity(Identity identity) throws Exception {
        identityRepository.delete(identity);
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

    public IdentityRepository getIdentityRepository() {
        return identityRepository;
    }

    public ContactRepository getContactRepository() {
        return contactRepository;
    }

 }
