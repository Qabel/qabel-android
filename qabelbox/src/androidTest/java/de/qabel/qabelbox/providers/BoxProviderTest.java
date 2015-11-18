package de.qabel.qabelbox.providers;

import android.test.ProviderTestCase2;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.exceptions.QblStorageException;
import de.qabel.core.storage.BoxFolder;
import de.qabel.core.storage.BoxNavigation;
import de.qabel.core.storage.BoxVolume;
import de.qabel.qabelbox.R;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BoxProviderTest extends ProviderTestCase2<BoxProvider>{

    private BoxVolume volume;
    final String bucket = "qabel";
    final String prefix = UUID.randomUUID().toString();

    public BoxProviderTest(Class<BoxProvider> providerClass, String providerAuthority) {
        super(providerClass, providerAuthority);
    }

    public BoxProviderTest() {
        this(BoxProvider.class, "de.qabel.qabelbox.providers.BoxProvider");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CryptoUtils utils = new CryptoUtils();
        byte[] deviceID = utils.getRandomBytes(16);

        QblECKeyPair keyPair = new QblECKeyPair();

        AWSCredentials awsCredentials = new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return getContext().getResources().getString(R.string.aws_user);
            }

            @Override
            public String getAWSSecretKey() {
                return getContext().getString(R.string.aws_password);
            }
        };
        AWSCredentials credentials = awsCredentials;
        AmazonS3Client s3Client = new AmazonS3Client(credentials);
        assertNotNull(awsCredentials.getAWSAccessKeyId());
        assertNotNull(awsCredentials.getAWSSecretKey());

        TransferUtility transfer = new TransferUtility(s3Client, getContext());
        volume = new BoxVolume(transfer, credentials, keyPair, bucket, prefix, deviceID,
                new File(System.getProperty("java.io.tmpdir")));
        volume.createIndex(bucket, prefix);
    }

    public void testInit() {
        assertThat(getProvider().getClass().getName(),
                is("de.qabel.qabelbox.providers.BoxProvider"));
    }

    public void testTraverseToFolder() throws QblStorageException {
        BoxProvider provider = getProvider();
        BoxNavigation rootNav = volume.navigate();
        BoxFolder folder = rootNav.createFolder("foobar");
        rootNav.commit();
        rootNav.createFolder("foobaz");
        rootNav.commit();
        List<BoxFolder> boxFolders = rootNav.listFolders();
        List<String> path = new ArrayList<>();
        path.add("");
        BoxNavigation navigation = provider.traverseToFolder(volume, path);
        assertThat(boxFolders, is(navigation.listFolders()));
        BoxNavigation nav1 = rootNav.navigate(folder);
        nav1.createFolder("blub");
        nav1.commit();
        path.add("foobar");
        navigation = provider.traverseToFolder(volume, path);
        assertThat("Could not navigate to /foobar/",
                nav1.listFolders(), is(navigation.listFolders()));

    }
}
