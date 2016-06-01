package de.qabel.qabelbox.providers;

import android.Manifest;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;

import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.UUID;

import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.exceptions.QblInvalidEncryptionKeyException;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.persistence.AndroidPersistence;
import de.qabel.qabelbox.persistence.QblSQLiteParams;
import de.qabel.qabelbox.persistence.RepositoryFactory;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.storage.BoxManager;
import de.qabel.qabelbox.util.BoxTestHelper;

public class MockBoxProvider extends BoxProvider {

    public byte[] deviceID;
    public String lastID;
    public QblECKeyPair keyPair;
    public String rootDocId;
    public static String prefix;
    public static final String PUB_KEY = "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a";
    public static final String PRIVATE_KEY = "77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a";
    public Identity identity;

    public void bindToContext(final Context context) throws Exception {
        setParametersForTests();
        attachInfoForTests(context);
    }

    private void setParametersForTests() {
        prefix = UUID.randomUUID().toString();
        CryptoUtils utils = new CryptoUtils();
        deviceID = utils.getRandomBytes(16);
        keyPair = new QblECKeyPair(Hex.decode(PRIVATE_KEY));
        rootDocId = PUB_KEY + BoxProvider.DOCID_SEPARATOR + prefix + BoxProvider.DOCID_SEPARATOR
                + BoxProvider.PATH_SEP;
        identity = new Identity("testuser", null, keyPair);
        ArrayList<String> prefixes = new ArrayList<>();
        prefixes.add(prefix);
        identity.setPrefixes(prefixes);
    }

    private void attachInfoForTests(Context context) throws Exception {
        ProviderInfo info = new ProviderInfo();
        info.authority = BuildConfig.APPLICATION_ID + AUTHORITY;
        info.exported = true;
        info.grantUriPermissions = true;
        info.readPermission = Manifest.permission.MANAGE_DOCUMENTS;
        info.writePermission = Manifest.permission.MANAGE_DOCUMENTS;
        attachInfo(context, info);
        RepositoryFactory factory = new RepositoryFactory(context);
        IdentityRepository repository = factory.getIdentityRepository(factory.getAndroidClientDatabase());
        Identities identities = repository.findAll();
        for(Identity stored : identities.getIdentities()){
            repository.delete(stored);
        }
        repository.save(identity);
        boxManager = BoxTestHelper.createBoxManager(context);
    }
}

