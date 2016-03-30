package de.qabel.qabelbox.storage;


import android.test.AndroidTestCase;
import android.util.Log;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblServerException;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.storage.TransferManager.BoxTransferListener;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;


public class TransferManagerTest extends AndroidTestCase {
    private static final String TAG = "TransferManagerTest";

    String prefix = "test"; // Don't touch at the moment the test-server only accepts this prefix in debug moder (using the magictoken)
    private String testFileNameOnServer;

    private File tempDir;
    TransferManager transferManager;

    public void configureTestServer() {
        new AppPreference(QabelBoxApplication.getInstance().getApplicationContext()).setToken(QabelBoxApplication.getInstance().getApplicationContext().getString(string.blockserver_magic_testtoken));
    }

    @Override
    @Before
    public void setUp() throws IOException, QblStorageException {
        configureTestServer();
        tempDir = new File(System.getProperty("java.io.tmpdir"), "testtmp");
        tempDir.mkdir();
        transferManager = new TransferManager(tempDir);
        testFileNameOnServer = "testfile_" + UUID.randomUUID();
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
            fail();
            return null;
        }
    }

    public File createEmptyTargetfile() {
        File file = null;
        try {
            file = File.createTempFile("targetfile", "test", tempDir);
        } catch (IOException e) {
            Log.e(TAG, "Could not create empty targetfile in " + tempDir);
            fail();
        }
        return file;
    }


    @Override
    @After
    public void tearDown() throws IOException {
        syncDelete(testFileNameOnServer);
        FileUtils.deleteDirectory(tempDir);
    }

    private int syncUpload(final String nameOnServer, final File sourceFile) {
        BoxTransferListener listner = new VerboseTransferManagerListener(sourceFile + " -> " + nameOnServer, "uploading");
        int transferId = transferManager.uploadAndDeleteLocalfileOnSuccess(prefix, nameOnServer, sourceFile, listner);
        transferManager.waitFor(transferId);
        return transferId;
    }

    private int syncDownload(final String nameOnServer, final File targetFile) {
        BoxTransferListener listner = new VerboseTransferManagerListener(nameOnServer + " -> " + targetFile, "uploading");
        int transferId = transferManager.download(prefix, nameOnServer, targetFile, listner);
        transferManager.waitFor(transferId);
        return transferId;
    }

    private int syncDelete(final String nameOnServer) {
        int transferId = transferManager.delete(prefix, nameOnServer);
        transferManager.waitFor(transferId);
        return transferId;
    }


    @Test
    public void testUpload() {
        File smallFileToUpload = null;
        smallFileToUpload = smallTestFile();
        int uploadId = syncUpload(testFileNameOnServer, smallFileToUpload);
        assertTransferManagerWasSuccesful(uploadId);
        assertFalse(smallFileToUpload.exists());
    }

    @Test
    public void testUploadBlock() {
        File smallFileToUpload = null;
        smallFileToUpload = smallTestFile();
        int uploadId = syncUpload("blocks/" + testFileNameOnServer, smallFileToUpload);
        assertTransferManagerWasSuccesful(uploadId);
        assertFalse(smallFileToUpload.exists());
    }

    @Test
    public void testDownload() {
        File sourceFile = smallTestFile();
        File sourceFileBackup = createEmptyTargetfile();
        try {
            FileUtils.copyFile(sourceFile, sourceFileBackup);
        } catch (IOException e) {
            fail();
        }
        String sourceFileOrig = sourceFile.getAbsolutePath();
        String sourceFileContent = null;
        try {
            sourceFileContent = FileUtils.readFileToString(sourceFile);
        } catch (IOException e) {
            fail("Could not find sourcefile: " + sourceFile.getAbsolutePath() + " " + e);
        }

        File targetFile = createEmptyTargetfile();
        syncUpload(testFileNameOnServer, sourceFile);
        assertEquals(sourceFileOrig, sourceFile.getAbsolutePath());
        assertFalse(sourceFile.exists());
        int transferId = syncDownload(testFileNameOnServer, targetFile);

        assertTransferManagerWasSuccesful(transferId);
        assertFileContentIsEqual(sourceFileBackup, targetFile);
    }

    @Test
    public void testDownloadMissingFile() {
        File targetFile = createEmptyTargetfile();
        int transferID = syncDownload(testFileNameOnServer + "_missing", targetFile);
        assertTransferHasServerError(transferID, 404);
        assertEquals(0, targetFile.length());

        targetFile.delete();
        transferID = syncDownload(testFileNameOnServer + "_missing", targetFile);
        assertTransferHasServerError(transferID, 404);
        assertFalse(targetFile.exists());
    }

    @Test
    public void testDeleteTwice() {
        String fileNameOnServer = testFileNameOnServer;
        File sourceFile = smallTestFile();
        File targetFile = createEmptyTargetfile();
        syncUpload(fileNameOnServer, sourceFile);
        int deleteId0 = syncDelete(fileNameOnServer);
        assertEquals(0, targetFile.length());

        int deleteId1 = syncDelete(fileNameOnServer);
        assertTransferManagerWasSuccesful(deleteId0);
        assertTransferManagerWasSuccesful(deleteId1);
        assertEquals(0, targetFile.length());

        int downloadId = syncDownload(fileNameOnServer, targetFile);
        assertTransferHasServerError(downloadId, 404);
        assertEquals(0, targetFile.length());
    }

    @Test
    public void testDeleteOnNonExistentTargetFile() {
        String fileNameOnServer = testFileNameOnServer + "_missing";
        File sourceFile = smallTestFile();
        File targetFile = createEmptyTargetfile();
        targetFile.delete();
        syncUpload(fileNameOnServer, sourceFile);
        int transferId = syncDelete(fileNameOnServer);
        assertTransferManagerWasSuccesful(transferId);
        assertFalse("Delete touched local file, which should not be created", targetFile.exists());
    }

    @Test
    public void testDelete() {
        String fileNameOnServer = testFileNameOnServer;
        File sourceFile = smallTestFile();
        File targetFile = createEmptyTargetfile();
        syncUpload(fileNameOnServer, sourceFile);
        int deleteId = syncDelete(fileNameOnServer);
        assertTransferManagerWasSuccesful(deleteId);
        assertEquals(0, targetFile.length());
    }

    public void assertTransferManagerWasSuccesful(int transferId) {
        assertNull(transferManager.lookupError(transferId));
    }

    public void assertTransferHasServerError(int transferId, int statusCode) {
        Exception error = transferManager.lookupError(transferId);
        assertNotNull(error);
        assertTrue(error instanceof QblServerException);
        assertEquals(statusCode, ((QblServerException) error).getStatusCode());
    }

    public void assertFileContentIsEqual(File lhs, File rhs) {
        try {
            String lhsContent = FileUtils.readFileToString(lhs);
            String rhsContent = FileUtils.readFileToString(rhs);
            assertTrue("File content not equal " + lhs.getName() + ": " + lhsContent + " vs " + rhs + ": " + rhsContent, FileUtils.contentEquals(lhs, rhs));
        } catch (IOException e) {
            fail("Exception during file comparison " + e);
        }
    }

    class VerboseTransferManagerListener implements BoxTransferListener {
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
