package de.qabel.qabelbox.storage;


import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.exceptions.QblStorageException;

import static junit.framework.Assert.assertEquals;


@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class TransferManagerTest extends AbstractTransferManagerTest {

    @Before
    public void setUp() throws IOException, QblStorageException {
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        configureTestServer();
        tempDir = new File(System.getProperty("java.io.tmpdir"), "testtmp");
        tempDir.mkdir();
        transferManager = new BlockServerTransferManager(tempDir);
        testFileNameOnServer = "testfile_" + UUID.randomUUID().toString();
    }

    @After
    public void tearDown() throws IOException {
        syncDelete(testFileNameOnServer);
        FileUtils.deleteDirectory(tempDir);
    }

    private File createTestFile(long kb) throws Exception {
        File testFile = createEmptyTargetFile();
        OutputStream outputStream = new FileOutputStream(testFile);
        byte[] testData = new byte[1024];
        Arrays.fill(testData, (byte) 'f');
        for (int i = 0; i < kb; i++) {
            outputStream.write(testData);
        }
        outputStream.close();
        return testFile;
    }

    @Test
    public void testUploadProgress() throws Exception {
        long kb = 100;
        File testFile = createTestFile(kb);
        long total = testFile.length();
        assertEquals(kb * 1024, total);
        final long[] status = {0, 0};
        int uploadId = transferManager.uploadAndDeleteLocalfileOnSuccess(prefix, testFileNameOnServer, testFile, new BoxTransferListener() {
            @Override
            public void onProgressChanged(long bytesCurrent, long bytesTotal) {
                status[0] = bytesCurrent;
                status[1]++;
                Log.d(TAG, "progress " + testFileNameOnServer + " " + bytesCurrent + "/" + bytesTotal);
                assertEquals(0L, bytesCurrent % 2048);
            }

            @Override
            public void onFinished() {
                assertEquals(total, status[0]);
            }
        });
        transferManager.waitFor(uploadId);
        assertEquals(total, status[0]);
        //2kb steps
        assertEquals(kb / 2, status[1]);
        assertTransferManagerWasSuccesful(uploadId);
        assertFalse(testFile.exists());
    }


}
