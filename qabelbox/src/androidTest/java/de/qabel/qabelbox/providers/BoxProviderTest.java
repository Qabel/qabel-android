package de.qabel.qabelbox.providers;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.test.InstrumentationTestCase;
import android.test.mock.MockContentResolver;
import android.util.Log;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.storage.BoxFolder;
import de.qabel.qabelbox.storage.BoxNavigation;
import de.qabel.qabelbox.storage.BoxVolume;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BoxProviderTest extends InstrumentationTestCase {

    private BoxVolume volume;
    final String bucket = BoxProvider.BUCKET;
    private String testFileName;
    private MockContentResolver mContentResolver;
    public static String ROOT_DOC_ID;

    private static final String TAG = "BoxProviderTest";
    private BoxProviderTester mProvider;
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Log.d(TAG, "setUp");

        mContext = getInstrumentation().getTargetContext();
        mProvider = new BoxProviderTester();
        mProvider.bindToService(mContext);
        mContentResolver = new MockContentResolver();
        mContentResolver.addProvider(BoxProvider.AUTHORITY, mProvider);
        byte[] deviceID = getProvider().deviceID;
        BoxProviderTester provider = getProvider();
        ROOT_DOC_ID = provider.rootDocId;
        provider.transferUtility = new TransferUtility(provider.amazonS3Client, mContext);

        volume = new BoxVolume(provider.transferUtility, provider.awsCredentials,
                provider.keyPair, bucket, provider.prefix, deviceID, mContext);
        volume.createIndex(bucket, provider.prefix);

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

    BoxProviderTester getProvider() {
        return mProvider;
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        Log.d(TAG, "tearDown");
        ObjectListing listing = getProvider().amazonS3Client.listObjects(bucket, getProvider().prefix);
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
        rootNav.navigate(folder);
        rootNav.createFolder("blub");
        rootNav.commit();
        path.add("foobar");
        navigation = provider.traverseToFolder(volume, path);
        assertThat("Could not navigate to /foobar/",
                rootNav.listFolders(), is(navigation.listFolders()));

    }

    public void testQueryRoots() throws FileNotFoundException {
        Cursor cursor = getProvider().queryRoots(BoxProvider.DEFAULT_ROOT_PROJECTION);
        assertThat(cursor.getCount(), is(1));
        cursor.moveToFirst();
        String documentId = cursor.getString(6);
        assertThat(documentId, is(BoxProviderTester.PUB_KEY + MainActivity.HARDCODED_ROOT));

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
    public void testOpenDocumentForRW() throws IOException, QblStorageException, InterruptedException {
        // Upload a test payload
        BoxNavigation rootNav = volume.navigate();
        byte[] testContent = new byte[] {0,1,2,3,4,5};
        byte[] updatedContent = new byte[] {0,1,2,3,4,5,6};
        rootNav.upload("testfile", new ByteArrayInputStream(testContent), null);
        rootNav.commit();

        // Check if the test playload can be read
        String testDocId = ROOT_DOC_ID + "testfile";
        Uri documentUri = DocumentsContract.buildDocumentUri(BoxProvider.AUTHORITY, testDocId);
        assertNotNull("Could not build document URI", documentUri);
        ParcelFileDescriptor parcelFileDescriptor =
                mContentResolver.openFileDescriptor(documentUri, "rw");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

        InputStream inputStream = new FileInputStream(fileDescriptor);
        byte[] dl = IOUtils.toByteArray(inputStream);
        assertThat("Downloaded file not correct", dl, is(testContent));

        // Use the same file descriptor to upload new content
        OutputStream outputStream = new FileOutputStream(fileDescriptor);
        assertNotNull(outputStream);
        outputStream.write(6);
        outputStream.close();
        parcelFileDescriptor.close();

        // wait for the upload in the background
        // TODO: actually wait for it.
        Thread.sleep(4000L);

        // check the uploaded new content
        InputStream dlInputStream = mContentResolver.openInputStream(documentUri);
        assertNotNull(inputStream);
        byte[] downloaded = IOUtils.toByteArray(dlInputStream);
        assertThat("Changes to the uploaded file not found", downloaded, is(updatedContent));
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
        assertThat(navigate.getPath(folder), is("/testfolder/"));
        navigate.commit();
        navigate.navigate(folder);
        assertThat(volume.getDocumentId(navigate.getPath()), is(ROOT_DOC_ID + "testfolder/"));
    }

}
