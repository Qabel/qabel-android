package de.qabel.qabelbox.providers;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;

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

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.communication.VolumeFileTransferHelper;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.helper.MockedBoxProviderTest;
import de.qabel.qabelbox.storage.BoxFolder;
import de.qabel.qabelbox.storage.BoxNavigation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

public class BoxProviderTest extends MockedBoxProviderTest {

    private String testFileName;

    private static final String TAG = "BoxProviderTest";
    private Context mContext;

    @Override
    public Context getContext() {
        return mContext;
    }

    @Override
    public void setUp() throws Exception {
        Log.d(TAG, "setUp");
        mContext = getInstrumentation().getTargetContext();
        super.setUp();

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


    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        Log.d(TAG, "tearDown");
    }

    public void testTraverseToFolder() throws QblStorageException {
        BoxProvider provider = getProvider();
        BoxNavigation rootNav = getVolume().navigate();
        BoxFolder folder = rootNav.createFolder("foobar");
        rootNav.commit();
        rootNav.createFolder("foobaz");
        rootNav.commit();
        List<BoxFolder> boxFolders = rootNav.listFolders();
        List<String> path = new ArrayList<>();
        path.add("");
        BoxNavigation navigation = provider.traverseToFolder(getVolume(), path);
        assertThat(boxFolders, is(navigation.listFolders()));
        rootNav.navigate(folder);
        rootNav.createFolder("blub");
        rootNav.commit();
        path.add("foobar");
        navigation = provider.traverseToFolder(getVolume(), path);
        assertThat("Could not navigate to /foobar/",
                rootNav.listFolders(), is(navigation.listFolders()));

    }

    public void testQueryRoots() throws FileNotFoundException {
        Cursor cursor = getProvider().queryRoots(BoxProvider.DEFAULT_ROOT_PROJECTION);
        assertThat(cursor.getCount(), is(1));
        cursor.moveToFirst();
        String documentId = cursor.getString(6);
        assertThat(documentId, startsWith(MockBoxProvider.PUB_KEY));

    }

    public void testOpenDocument() throws IOException, QblStorageException {
        BoxNavigation rootNav = getVolume().navigate();
        rootNav.upload("testfile", new FileInputStream(new File(testFileName)), null);
        rootNav.commit();
        assertThat(rootNav.listFiles().size(), is(1));
        String testDocId = ROOT_DOC_ID + "testfile";
        Uri documentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, testDocId);
        assertNotNull("Could not build document URI", documentUri);
        Cursor query = mockContentResolver.query(documentUri, null, null, null, null);
        assertNotNull("Document query failed: " + documentUri.toString(), query);
        assertTrue(query.moveToFirst());
        InputStream inputStream = mockContentResolver.openInputStream(documentUri);
        byte[] dl = IOUtils.toByteArray(inputStream);
        File file = new File(testFileName);
        byte[] content = IOUtils.toByteArray(new FileInputStream(file));
        assertThat(dl, is(content));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void testOpenDocumentForWrite() throws IOException, QblStorageException, InterruptedException {
        Uri parentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, ROOT_DOC_ID);
        Uri document = DocumentsContract.createDocument(mockContentResolver, parentUri,
                "image/png",
                "testfile");
        assertNotNull(document);
        OutputStream outputStream = mockContentResolver.openOutputStream(document);
        assertNotNull(outputStream);
        File file = new File(testFileName);
        IOUtils.copy(new FileInputStream(file), outputStream);
        outputStream.close();

        Thread.sleep(1000L);

        InputStream inputStream = mockContentResolver.openInputStream(document);
        assertNotNull(inputStream);
        byte[] dl = IOUtils.toByteArray(inputStream);
        byte[] content = IOUtils.toByteArray(new FileInputStream(file));
        assertThat(dl, is(content));
        assertTrue(getProvider().isBroadcastNotificationCalled);
        assertTrue(getProvider().isShowNotificationCalled);
        assertTrue(getProvider().isUpdateNotificationCalled);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void testOpenDocumentForRW() throws IOException, QblStorageException, InterruptedException {
        // Upload a test payload
        BoxNavigation rootNav = getVolume().navigate();
        byte[] testContent = new byte[]{0, 1, 2, 3, 4, 5};
        byte[] updatedContent = new byte[]{0, 1, 2, 3, 4, 5, 6};
        rootNav.upload("testfile", new ByteArrayInputStream(testContent), null);
        rootNav.commit();

        // Check if the test playload can be read
        String testDocId = ROOT_DOC_ID + "testfile";
        Uri documentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, testDocId);
        assertNotNull("Could not build document URI", documentUri);
        ParcelFileDescriptor parcelFileDescriptor =
                mockContentResolver.openFileDescriptor(documentUri, "rw");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

        InputStream inputStream = new FileInputStream(fileDescriptor);
        byte[] dl = IOUtils.toByteArray(inputStream);
        assertThat("Downloaded file not correct", dl, is(testContent));

        // Use the same file descriptor to uploadAndDeleteLocalfile new content
        OutputStream outputStream = new FileOutputStream(fileDescriptor);
        assertNotNull(outputStream);
        outputStream.write(6);
        outputStream.close();
        parcelFileDescriptor.close();

        Thread.sleep(1000L);

        // check the uploaded new content
        InputStream dlInputStream = mockContentResolver.openInputStream(documentUri);
        assertNotNull(inputStream);
        byte[] downloaded = IOUtils.toByteArray(dlInputStream);
        assertThat("Changes to the uploaded file not found", downloaded, is(updatedContent));
        assertTrue(getProvider().isBroadcastNotificationCalled);
        assertTrue(getProvider().isShowNotificationCalled);
        assertTrue(getProvider().isUpdateNotificationCalled);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void testCreateFile() {
        String testDocId = ROOT_DOC_ID + "testfile.png";
        Uri parentDocumentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, ROOT_DOC_ID);
        Uri documentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, testDocId);
        assertNotNull("Could not build document URI", documentUri);
        Cursor query = mockContentResolver.query(documentUri, null, null, null, null);
        assertNull("Document already there: " + documentUri.toString(), query);
        Uri document = DocumentsContract.createDocument(mockContentResolver, parentDocumentUri,
                "image/png",
                "testfile.png");
        assertNotNull("Create document failed, no document Uri returned", document);
        assertThat(document.toString(), is(documentUri.toString()));
        query = mockContentResolver.query(documentUri, null, null, null, null);
        assertNotNull("Document not created:" + documentUri.toString(), query);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void testDeleteFile() {
        String testDocId = ROOT_DOC_ID + "testfile.png";
        Uri parentDocumentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, ROOT_DOC_ID);
        Uri documentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, testDocId);
        assertNotNull("Could not build document URI", documentUri);
        Cursor query = mockContentResolver.query(documentUri, null, null, null, null);
        assertNull("Document already there: " + documentUri.toString(), query);
        Uri document = DocumentsContract.createDocument(mockContentResolver, parentDocumentUri,
                "image/png",
                "testfile.png");
        assertNotNull(document);
        DocumentsContract.deleteDocument(mockContentResolver, document);
        assertThat(document.toString(), is(documentUri.toString()));
        query = mockContentResolver.query(documentUri, null, null, null, null);
        assertNull("Document not deleted:" + documentUri.toString(), query);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void testRenameFile() {
        String testDocId = ROOT_DOC_ID + "testfile.png";
        Uri parentDocumentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, ROOT_DOC_ID);
        Uri documentUri = DocumentsContract.buildDocumentUri(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY, testDocId);
        assertNotNull("Could not build document URI", documentUri);
        Cursor query = mockContentResolver.query(documentUri, null, null, null, null);
        assertNull("Document already there: " + documentUri.toString(), query);
        Uri document = DocumentsContract.createDocument(
                mockContentResolver, parentDocumentUri,
                "image/png",
                "testfile.png");
        assertNotNull(document);
        Uri renamed = DocumentsContract.renameDocument(mockContentResolver,
                document, "testfile2.png");
        assertNotNull(renamed);
        assertThat(renamed.toString(), is(parentDocumentUri.toString() + "testfile2.png"));
        query = mockContentResolver.query(documentUri, null, null, null, null);
        assertNull("Document not renamed:" + documentUri.toString(), query);
        query = mockContentResolver.query(renamed, null, null, null, null);
        assertNotNull("Document not renamed:" + documentUri.toString(), query);
    }

    public void testGetDocumentId() throws QblStorageException {
        assertThat(getVolume().getDocumentId("/"), is(ROOT_DOC_ID));
        BoxNavigation navigate = getVolume().navigate();
        assertThat(getVolume().getDocumentId(navigate.getPath()), is(ROOT_DOC_ID));
        BoxFolder folder = navigate.createFolder("testfolder");
        assertThat(navigate.getPath(folder), is("/testfolder/"));
        navigate.commit();
        navigate.navigate(folder);
        assertThat(getVolume().getDocumentId(navigate.getPath()), is(ROOT_DOC_ID + "testfolder/"));
    }

}
