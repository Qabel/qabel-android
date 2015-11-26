package de.qabel.qabelbox.providers;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.test.ProviderTestCase2;
import android.util.Log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.apache.commons.io.IOUtils;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.exceptions.QblStorageException;
import de.qabel.core.storage.BoxFile;
import de.qabel.core.storage.BoxFolder;
import de.qabel.core.storage.BoxNavigation;
import de.qabel.core.storage.BoxVolume;
import de.qabel.qabelbox.activities.MainActivity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BoxProviderTest extends ProviderTestCase2<BoxProvider>{

    private BoxVolume volume;
    final String bucket = BoxProvider.BUCKET;
    final String prefix = UUID.randomUUID().toString();
    public static String ROOT_DOC_ID;
    private String testFileName;
    private ContentResolver mContentResolver;

    private static final String TAG = "BoxProviderTest";

    public BoxProviderTest(Class<BoxProvider> providerClass, String providerAuthority) {
        super(providerClass, providerAuthority);
    }

    public BoxProviderTest() {
        this(BoxProvider.class, BoxProvider.AUTHORITY);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Log.d(TAG, "setUp");
        CryptoUtils utils = new CryptoUtils();
        byte[] deviceID = utils.getRandomBytes(16);
        QblECKeyPair keyPair = new QblECKeyPair(Hex.decode(BoxProvider.PRIVATE_KEY));
        ROOT_DOC_ID = BoxProvider.PUB_KEY + BoxProvider.DOCID_SEPARATOR + BoxProvider.BUCKET
                + BoxProvider.DOCID_SEPARATOR + prefix + BoxProvider.DOCID_SEPARATOR
                + BoxProvider.PATH_SEP;
        BoxProvider provider = getProvider();
        provider.transferUtility = new TransferUtility(provider.amazonS3Client, getContext());

        volume = new BoxVolume(provider.transferUtility, provider.awsCredentials,
                keyPair, bucket, prefix, deviceID,
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

        mContentResolver = getContext().getContentResolver();

    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        Log.d(TAG, "tearDown");
        ObjectListing listing = getProvider().amazonS3Client.listObjects(bucket, prefix);
        List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<>();
        for (S3ObjectSummary summary : listing.getObjectSummaries()) {
            keys.add(new DeleteObjectsRequest.KeyVersion(summary.getKey()));
        }
        if (keys.isEmpty()) {
            return;
        }
        DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucket);
        deleteObjectsRequest.setKeys(keys);
        getProvider().amazonS3Client.deleteObjects(deleteObjectsRequest);
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
        assertThat(documentId, is(MainActivity.HARDCODED_ROOT));

    }

    public void testOpenDocument() throws IOException, QblStorageException {
        BoxNavigation rootNav = volume.navigate();
        rootNav.upload("testfile", new FileInputStream(new File(testFileName)), null);
        rootNav.commit();
        assertThat(rootNav.listFiles().size(), is(1));
        String testDocId = ROOT_DOC_ID + "testfile";
        Uri documentUri = DocumentsContract.buildDocumentUri(BoxProvider.AUTHORITY, testDocId);
        assertNotNull("Could not build document URI", documentUri);
        Cursor query = mContentResolver.query(documentUri, null, null, null, null);
        assertNotNull("Document query failed: " + documentUri.toString(), query);
        assertTrue(query.moveToFirst());
        InputStream inputStream = mContentResolver.openInputStream(documentUri);
        byte[] dl = IOUtils.toByteArray(inputStream);
        File file = new File(testFileName);
        byte[] content = IOUtils.toByteArray(new FileInputStream(file));
        assertThat(dl, is(content));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void testOpenDocumentForWrite() throws IOException, QblStorageException, InterruptedException {
        Uri parentUri = DocumentsContract.buildDocumentUri(BoxProvider.AUTHORITY, ROOT_DOC_ID);
        Uri document = DocumentsContract.createDocument(mContentResolver, parentUri,
                "image/png",
                "testfile");
        assertNotNull(document);
        OutputStream outputStream = mContentResolver.openOutputStream(document);
        assertNotNull(outputStream);
        File file = new File(testFileName);
        IOUtils.copy(new FileInputStream(file), outputStream);
        outputStream.close();

        // wait for the upload in the background
        // TODO: actually wait for it.
        Thread.sleep(10000l);

        InputStream inputStream = mContentResolver.openInputStream(document);
        assertNotNull(inputStream);
        byte[] dl = IOUtils.toByteArray(inputStream);
        byte[] content = IOUtils.toByteArray(new FileInputStream(file));
        assertThat(dl, is(content));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void testCreateFile() {
        String testDocId = ROOT_DOC_ID + "testfile.png";
        Uri parentDocumentUri = DocumentsContract.buildDocumentUri(BoxProvider.AUTHORITY, ROOT_DOC_ID);
        Uri documentUri = DocumentsContract.buildDocumentUri(BoxProvider.AUTHORITY, testDocId);
        assertNotNull("Could not build document URI", documentUri);
        Cursor query = mContentResolver.query(documentUri, null, null, null, null);
        assertNull("Document already there: " + documentUri.toString(), query);
        Uri document = DocumentsContract.createDocument(mContentResolver, parentDocumentUri,
                "image/png",
                "testfile.png");
        assertNotNull(document);
        assertThat(document.toString(), is(documentUri.toString()));
        query = mContentResolver.query(documentUri, null, null, null, null);
        assertNotNull("Document not created:" + documentUri.toString(), query);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void testDeleteFile() {
        String testDocId = ROOT_DOC_ID + "testfile.png";
        Uri parentDocumentUri = DocumentsContract.buildDocumentUri(BoxProvider.AUTHORITY, ROOT_DOC_ID);
        Uri documentUri = DocumentsContract.buildDocumentUri(BoxProvider.AUTHORITY, testDocId);
        assertNotNull("Could not build document URI", documentUri);
        Cursor query = mContentResolver.query(documentUri, null, null, null, null);
        assertNull("Document already there: " + documentUri.toString(), query);
        Uri document = DocumentsContract.createDocument(mContentResolver, parentDocumentUri,
                "image/png",
                "testfile.png");
        assertNotNull(document);
        DocumentsContract.deleteDocument(mContentResolver, document);
        assertThat(document.toString(), is(documentUri.toString()));
        query = mContentResolver.query(documentUri, null, null, null, null);
        assertNull("Document not deleted:" + documentUri.toString(), query);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void testRenameFile() {
        String testDocId = ROOT_DOC_ID + "testfile.png";
        Uri parentDocumentUri = DocumentsContract.buildDocumentUri(BoxProvider.AUTHORITY, ROOT_DOC_ID);
        Uri documentUri = DocumentsContract.buildDocumentUri(BoxProvider.AUTHORITY, testDocId);
        assertNotNull("Could not build document URI", documentUri);
        Cursor query = mContentResolver.query(documentUri, null, null, null, null);
        assertNull("Document already there: " + documentUri.toString(), query);
        Uri document = DocumentsContract.createDocument(
                mContentResolver, parentDocumentUri,
                "image/png",
                "testfile.png");
        assertNotNull(document);
        Uri renamed = DocumentsContract.renameDocument(mContentResolver,
                document, "testfile2.png");
        assertNotNull(renamed);
        assertThat(renamed.toString(), is(parentDocumentUri.toString() + "testfile2.png"));
        query = mContentResolver.query(documentUri, null, null, null, null);
        assertNull("Document not renamed:" + documentUri.toString(), query);
        query = mContentResolver.query(renamed, null, null, null, null);
        assertNotNull("Document not renamed:" + documentUri.toString(), query);
    }

    public void testGetDocumentId() throws QblStorageException {
        assertThat(volume.getDocumentId("/"), is(ROOT_DOC_ID));
        BoxNavigation navigate = volume.navigate();
        assertThat(volume.getDocumentId(navigate.getPath()), is(ROOT_DOC_ID));
        BoxFolder folder = navigate.createFolder("testfolder");
        assertThat(navigate.getPath(folder), is("/testfolder"));
        navigate.commit();
        BoxNavigation testFolderNav = navigate.navigate(folder);
        assertThat(volume.getDocumentId(testFolderNav.getPath()), is(ROOT_DOC_ID + "/testfolder"));
    }

}
