package de.qabel.qabelbox.storage;


import android.test.AndroidTestCase;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblStorageException;

public class TransferManagerTest extends AndroidTestCase {
    private static final String TAG = "TransferManagerTest";

    String prefix = "test"; // Don't touch at the moment the test-server only accepts this prefix in debug moder (using the magictoken)
    private String testFileNameOnServer;

    private File tempDir;
    TransferManager transferManager;

    public void configureTestServer() {
        new AppPreference(QabelBoxApplication.getInstance().getApplicationContext()).setToken(QabelBoxApplication.getInstance().getApplicationContext().getString(R.string.blockserver_magic_testtoken));
    }

    @Before
    public void setUp() throws IOException, QblStorageException {
        configureTestServer();

        tempDir = new File(System.getProperty("java.io.tmpdir"), "testtmp");
        tempDir.mkdir();
        transferManager = new TransferManager(tempDir);

        testFileNameOnServer = "testfile_" + UUID.randomUUID().toString();
    }


    public BoxVolume getVolumeForRoot(Identity identity, byte[] deviceID, String prefix) {
        if (identity == null) {
            throw new NullPointerException("Identity is null");
        }
        QblECKeyPair key = identity.getPrimaryKeyPair();
        return new BoxVolume(key, prefix,
                deviceID, getContext());
    }

    public static String createTestFile() throws IOException {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File file = File.createTempFile("testfile", "test", tmpDir);
        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] testData = new byte[1024];
        Arrays.fill(testData, (byte) 'f');
        for (int i = 0; i < 100; i++) {
            outputStream.write(testData);
        }
        outputStream.close();
        return file.getAbsolutePath();
    }

    public static File smallTestFile() {
        try {
            File tmpDir = new File(System.getProperty("java.io.tmpdir"));
            File file = File.createTempFile("testfile", "test", tmpDir);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] testData = new byte[]{1, 2, 3, 4, 5};
            outputStream.write(testData);
            outputStream.close();
            return file;
        } catch (IOException e) {
            Log.e(TAG, "Could not create small test file", e);
            return null;
        }
    }

    public File createEmptyTargetfile() {
        File file = null;
        try {
            file = File.createTempFile("testfile", "test", tempDir);
        } catch (IOException e) {
            Log.e(TAG, "Could not create empty targetfile in " + tempDir);
        }
        return file;
    }


    public void tearDown() throws IOException {
        transferManager.delete(prefix, testFileNameOnServer);
        FileUtils.deleteDirectory(tempDir);
    }

    private int syncUpload(final String nameOnServer, final File sourceFile) {
        TransferManager.BoxTransferListener listner = new VerboseTransferManagerListener(sourceFile + " -> " + nameOnServer, "uploading");
        int transferId = transferManager.upload(prefix, nameOnServer, sourceFile, listner);
        transferManager.waitFor(transferId);
        return transferId;
    }

    private int syncDownload(final String nameOnServer, final File targetFile) {
        TransferManager.BoxTransferListener listner = new VerboseTransferManagerListener(nameOnServer + " -> " + targetFile, "uploading");
        int transferId = transferManager.upload(prefix, nameOnServer, targetFile, listner);
        transferManager.waitFor(transferId);
        return transferId;
    }


    @Test
    public void testUpload() {
        File smallFileToUpload = null;
        smallFileToUpload = smallTestFile();
        syncUpload(testFileNameOnServer, smallFileToUpload);
    }

    @Test
    public void testDownload() {
        File sourceFile = smallTestFile();
        File targetFile = createEmptyTargetfile();
        syncUpload(testFileNameOnServer, sourceFile);
        syncDownload(testFileNameOnServer, targetFile);
        assertFileContentIsEqual(sourceFile, targetFile);
    }

    @Ignore // TODO: implement
    @Test
    public void testDelete() {
        String fileNameOnServer = "testfile.data";
        File sourceFile = smallTestFile();
        File targetFile = createEmptyTargetfile();
        syncUpload(fileNameOnServer, sourceFile);
        transferManager.delete(prefix, fileNameOnServer);

        int transferId = syncDownload(fileNameOnServer, targetFile);
        assertNotNull("Expected Error not found", transferManager.lookupError(transferId));
        assertFalse(targetFile.exists());

        // Deleting twice is ok
        transferManager.delete(prefix, fileNameOnServer);
    }

    public void assertFileContentIsEqual(File lhs, File rhs) {
        try {
            assertTrue("File content not equal", FileUtils.contentEquals(lhs, rhs));
        } catch (IOException e) {
            fail("Exception during file comparison " + e);
        }
    }

    class VerboseTransferManagerListener implements TransferManager.BoxTransferListener {

        String fileName;
        String mode;

        public VerboseTransferManagerListener(String fileName, String mode) {
            this.fileName = fileName;
            this.mode = mode;
        }

        @Override
        public void onProgressChanged(long bytesCurrent, long bytesTotal) {
            Log.d(TAG, "progress " + mode + " " + fileName + " " + bytesCurrent + "/" + bytesTotal);
        }

        @Override
        public void onFinished() {
            Log.d(TAG, "done " + mode + " " + fileName);
        }
    }


}
