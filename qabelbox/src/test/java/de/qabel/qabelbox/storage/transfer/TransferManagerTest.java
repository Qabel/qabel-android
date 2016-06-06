package de.qabel.qabelbox.storage.transfer;


import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.storage.transfer.AbstractTransferManagerTest;
import de.qabel.qabelbox.storage.transfer.BlockServerTransferManager;
import de.qabel.qabelbox.storage.server.AndroidBlockServer;

@Ignore("The test servers are flaky")
@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class TransferManagerTest extends AbstractTransferManagerTest {

    @Before
    public void setUp() throws IOException, QblStorageException {
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        configureTestServer();
        tempDir = new File(System.getProperty("java.io.tmpdir"), "testtmp");
        tempDir.mkdir();
        transferManager = new BlockServerTransferManager(RuntimeEnvironment.application,
                new AndroidBlockServer(
                        new AppPreference(RuntimeEnvironment.application), RuntimeEnvironment.application),
                tempDir);
        testFileNameOnServer = "testfile_" + UUID.randomUUID().toString();

    }

    @After
    public void tearDown() throws IOException {
        syncDelete(testFileNameOnServer);
        FileUtils.deleteDirectory(tempDir);
    }

}
