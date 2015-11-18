package de.qabel.qabelbox.providers;

import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.test.ProviderTestCase2;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;

import org.apache.commons.io.IOUtils;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
    public static String ROOT_DOC_ID;
    private String testFileName;

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

        QblECKeyPair keyPair = new QblECKeyPair(Hex.decode(
                "77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a"));
        ROOT_DOC_ID = "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a::::qabel::::"
                +prefix+"::::/";

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

        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File file = File.createTempFile("testfile", "test", tmpDir);
        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] testData = new byte[1024];
        Arrays.fill(testData, (byte) 'f');
        for (int i = 0; i < 100; i++) {
            outputStream.write(testData);
        }
        outputStream.close();
        testFileName = file.getAbsolutePath();

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

    public void testQueryRoots() throws FileNotFoundException {
        Cursor cursor = getProvider().queryRoots(BoxProvider.DEFAULT_ROOT_PROJECTION);
        assertThat(cursor.getCount(), is(1));
        cursor.moveToFirst();
        String documentId = cursor.getString(6);
        assertThat(documentId, is(ROOT_DOC_ID));

    }

    public void testOpenDocument() throws IOException, QblStorageException {
        BoxNavigation rootNav = volume.navigate();
        rootNav.upload("testfile", new FileInputStream(new File(testFileName)));
        rootNav.commit();
        assertThat(rootNav.listFiles().size(), is(1));
        String testDocId = ROOT_DOC_ID + "testfile";
        Uri documentUri = DocumentsContract.buildDocumentUri(BoxProvider.AUTHORITY, testDocId);
        assertNotNull("Could not build odcument URI", documentUri);
        Cursor query = getContext().getContentResolver().query(documentUri, null, null, null, null);
        //Cursor query = getMockContentResolver().query(documentUri, null, null, null, null);
        assertNotNull("Document query failed: "+documentUri.toString(), query);
        assertTrue(query.moveToFirst());
        InputStream inputStream = getContext().getContentResolver().openInputStream(documentUri);
        byte[] dl = IOUtils.toByteArray(inputStream);
        File file = new File(testFileName);
        byte[] content = IOUtils.toByteArray(new FileInputStream(file));
        assertThat(dl, is(content));
    }

}
