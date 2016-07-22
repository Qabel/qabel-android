package de.qabel.qabelbox.helper;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.mock.MockContentResolver;

import org.junit.Before;

import java.io.FileNotFoundException;

import de.qabel.box.storage.BoxVolume;
import de.qabel.box.storage.exceptions.QblStorageException;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.providers.BoxProvider;
import de.qabel.qabelbox.providers.MockBoxProvider;

public abstract class MockedBoxProviderTest extends InstrumentationTestCase {

    MockBoxProvider mockProvider;
    protected MockContentResolver mockContentResolver;
    private BoxVolume volume;
    public static String ROOT_DOC_ID;


    public abstract Context getContext();

    @Before
    public void setUp() throws Exception {
        configureTestServer();
        initMockContext();
        initRootVolume();

    }

    protected void configureTestServer() {

        new AppPreference(QabelBoxApplication.getInstance()).setToken(TestConstants.TOKEN);
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);


    }

    private void initMockContext() throws Exception {
        mockProvider = new MockBoxProvider();
        mockProvider.bindToContext(getInstrumentation().getTargetContext());
        mockContentResolver = new MockContentResolver();
        mockContentResolver.addProvider(BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY,
                mockProvider);
    }

    private void initRootVolume() throws QblStorageException, FileNotFoundException {
        MockBoxProvider provider = getProvider();
        String keyIdentifier = provider.keyPair.getPub().getReadableKeyIdentifier();
        volume = provider.getVolumeForRoot(keyIdentifier, MockBoxProvider.prefix);
        //volume.createIndex(volume.getRootRef());

        ROOT_DOC_ID = keyIdentifier + "::::" + MockBoxProvider.prefix + "::::/";
    }

    protected MockBoxProvider getProvider() {
        return mockProvider;
    }

    public BoxVolume getVolume() {
        return volume;
    }

}
