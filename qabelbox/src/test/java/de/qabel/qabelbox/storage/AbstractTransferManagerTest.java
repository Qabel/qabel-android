package de.qabel.qabelbox.storage;

import android.support.annotation.NonNull;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblServerException;
import de.qabel.qabelbox.storage.transfer.BoxTransferListener;
import de.qabel.qabelbox.storage.transfer.TransferManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public abstract class AbstractTransferManagerTest {
    static final String TAG = "TransferManagerTest";
    protected String testFileNameOnServer;
    protected File tempDir;
    String prefix = "test"; // Don't touch at the moment the test-server only accepts this prefix in debug moder (using the magictoken)
    TransferManager transferManager;

    @NonNull
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
            throw new AssertionError("Could not create small test file");
        }
    }

    private static void fail(String message) {
        throw new AssertionError(message);
    }

    private static void fail() {
        throw new AssertionError();
    }

    public void configureTestServer() {
        new AppPreference(QabelBoxApplication.getInstance()).setToken(TestConstants.TOKEN);
    }

    @NonNull
    public File createEmptyTargetFile() {
        try {
            return File.createTempFile("targetfile", "test", tempDir);
        } catch (IOException e) {
            Log.e(TAG, "Could not create empty targetfile in " + tempDir);
            throw new AssertionError("Could not create empty test file");
        }
    }

    protected int syncUpload(final String nameOnServer, final File sourceFile) {
        BoxTransferListener listener = new VerboseTransferManagerListener(sourceFile + " -> " + nameOnServer, "uploading");
        int transferId = transferManager.uploadAndDeleteLocalfileOnSuccess(prefix, nameOnServer, sourceFile, listener);
        transferManager.waitFor(transferId);
        return transferId;
    }

    private int syncDownload(final String nameOnServer, final File targetFile) {
        BoxTransferListener listener = new VerboseTransferManagerListener(nameOnServer + " -> " + targetFile, "uploading");
        int transferId = transferManager.download(prefix, nameOnServer, targetFile, listener);
        transferManager.waitFor(transferId);
        return transferId;
    }

    protected int syncDelete(final String nameOnServer) {
        int transferId = transferManager.delete(prefix, nameOnServer);
        transferManager.waitFor(transferId);
        return transferId;
    }

    protected void assertFalse(boolean assertion) {
        assertThat(assertion, is(false));
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
        File sourceFileBackup = createEmptyTargetFile();
        try {
            FileUtils.copyFile(sourceFile, sourceFileBackup);
        } catch (IOException e) {
            fail();
        }
        String sourceFileOrig = sourceFile.getAbsolutePath();
        try {
            FileUtils.readFileToString(sourceFile);
        } catch (IOException e) {
            fail("Could not find sourcefile: " + sourceFile.getAbsolutePath() + " " + e);
        }

        File targetFile = createEmptyTargetFile();
        syncUpload(testFileNameOnServer, sourceFile);
        assertThat(sourceFileOrig, equalTo(sourceFile.getAbsolutePath()));
        assertFalse(sourceFile.exists());
        int transferId = syncDownload(testFileNameOnServer, targetFile);

        assertTransferManagerWasSuccesful(transferId);
        assertFileContentIsEqual(sourceFileBackup, targetFile);
    }

    @Test
    public void testDownloadMissingFile() {
        File targetFile = createEmptyTargetFile();
        int transferID = syncDownload(testFileNameOnServer + "_missing", targetFile);
        assertTransferHasServerError(transferID, 404);
        assertThat(transferManager.waitFor(transferID), is(false));
        assertThat(targetFile.length(), equalTo(0L));

        targetFile.delete();
        transferID = syncDownload(testFileNameOnServer + "_missing", targetFile);
        assertTransferHasServerError(transferID, 404);
        assertThat(transferManager.waitFor(transferID), is(false));
        assertFalse(targetFile.exists());
    }

    @Test
    public void testDeleteTwice() {
        String fileNameOnServer = testFileNameOnServer;
        File sourceFile = smallTestFile();
        File targetFile = createEmptyTargetFile();
        syncUpload(fileNameOnServer, sourceFile);
        int deleteId0 = syncDelete(fileNameOnServer);
        assertThat(targetFile.length(), equalTo(0L));

        int deleteId1 = syncDelete(fileNameOnServer);
        assertTransferManagerWasSuccesful(deleteId0);
        assertTransferManagerWasSuccesful(deleteId1);
        assertThat(targetFile.length(), equalTo(0L));

        int downloadId = syncDownload(fileNameOnServer, targetFile);
        assertTransferHasServerError(downloadId, 404);
        assertThat(targetFile.length(), equalTo(0L));
    }

    @Test
    public void testDeleteOnNonExistentTargetFile() {
        String fileNameOnServer = testFileNameOnServer + "_missing";
        File sourceFile = smallTestFile();
        File targetFile = createEmptyTargetFile();
        targetFile.delete();
        syncUpload(fileNameOnServer, sourceFile);
        int transferId = syncDelete(fileNameOnServer);
        assertTransferManagerWasSuccesful(transferId);
        assertThat("Delete touched local file, which should not be created",
                targetFile.exists(), is(false));
    }

    @Test
    public void testDelete() {
        String fileNameOnServer = testFileNameOnServer;
        File sourceFile = smallTestFile();
        File targetFile = createEmptyTargetFile();
        syncUpload(fileNameOnServer, sourceFile);
        int deleteId = syncDelete(fileNameOnServer);
        assertTransferManagerWasSuccesful(deleteId);
        assertThat(targetFile.length(), equalTo(0L));
    }

    public void assertTransferManagerWasSuccesful(int transferId) {
        assertThat(transferManager.lookupError(transferId), nullValue());
    }

    public void assertTransferHasServerError(int transferId, int statusCode) {
        Exception error = transferManager.lookupError(transferId);
        assertThat(error, notNullValue());
        assertThat(error, instanceOf(QblServerException.class));
        assertThat(statusCode, equalTo(((QblServerException) error).getStatusCode()));
    }

    public void assertFileContentIsEqual(File lhs, File rhs) {
        try {
            String lhsContent = FileUtils.readFileToString(lhs);
            String rhsContent = FileUtils.readFileToString(rhs);
            assertThat("File content not equal " + lhs.getName() + ": " + lhsContent + " vs " + rhs + ": " + rhsContent, FileUtils.contentEquals(lhs, rhs));
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
