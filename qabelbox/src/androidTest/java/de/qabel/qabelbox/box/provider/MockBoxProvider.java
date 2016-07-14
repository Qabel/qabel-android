package de.qabel.qabelbox.box.provider;

import android.Manifest;
import android.content.Context;
import android.content.pm.ProviderInfo;

import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;

import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.persistence.RepositoryFactory;

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
        prefix = TestConstants.PREFIX;
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

        RepositoryFactory repositoryFactory = new RepositoryFactory(context);
        IdentityRepository repository = repositoryFactory.getIdentityRepository(repositoryFactory.getAndroidClientDatabase());
        Identities identities = repository.findAll();
        for(Identity stored : identities.getIdentities()){
            repository.delete(stored);
        }
        repository.save(identity);
    }
}

