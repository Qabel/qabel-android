package de.qabel.qabelbox.storage.transfer;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.test.files.FileHelper;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class FakeTransferManagerTest extends AbstractTransferManagerTest {

    @Before
    public void setUp() throws IOException, QblStorageException {
        configureTestServer();
        tempDir = new File(System.getProperty("java.io.tmpdir"), "testtmp");
        tempDir.mkdir();
        transferManager = new FakeTransferManager(tempDir);
        testFileNameOnServer = "testfile_" + UUID.randomUUID().toString();
    }


    @After
    public void tearDown() throws IOException {
        syncDelete(testFileNameOnServer);
        FileUtils.deleteDirectory(tempDir);
    }

    @Test
    public void testUploadProgress() throws Exception {
        long kb = 2000;
        File testFile = FileHelper.createTestFile(kb);
        final long[] status = {0, 0};
        int uploadId = transferManager.uploadAndDeleteLocalfileOnSuccess(prefix, testFileNameOnServer, testFile, new BoxTransferListener() {
            @Override
            public void onProgressChanged(long bytesCurrent, long bytesTotal) {
                status[0]++;
            }

            @Override
            public void onFinished() {
                status[1]++;
            }
        });
        transferManager.waitFor(uploadId);
        Assert.assertTrue("Progress not called", status[0] > 0);
        Assert.assertTrue("Finished not called", status[1] > 0);
    }

    @Test
    public void testDownloadProgress() throws Exception {
        long kb = 200;
        File sourceFile = FileHelper.createTestFile(kb);
        long total = sourceFile.length();
        File sourceBackupFile = FileHelper.createEmptyTargetFile();
        FileUtils.copyFile(sourceFile, sourceBackupFile);
        syncUpload(testFileNameOnServer, sourceFile);
        assertFalse(sourceFile.exists());

        File targetFile = FileHelper.createEmptyTargetFile();
        long[] progress = new long[]{0, 0};
        int downloadId = transferManager.download(prefix, testFileNameOnServer, targetFile, new BoxTransferListener() {

            @Override
            public void onProgressChanged(long bytesCurrent, long bytesTotal) {
                progress[0]++;
            }

            @Override
            public void onFinished() {
                progress[1] = 1;
            }
        });
        transferManager.waitFor(downloadId);
        assertEquals(total, targetFile.length());
        assertFileContentIsEqual(sourceBackupFile, targetFile);
        Assert.assertTrue("Progress not called", progress[0] > 0);
        assertTrue("Finished not called", progress[1] == 1);
    }

}
