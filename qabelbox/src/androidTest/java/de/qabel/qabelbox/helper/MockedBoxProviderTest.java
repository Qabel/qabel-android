package de.qabel.qabelbox.helper;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.test.InstrumentationTestCase;
import android.test.mock.MockContentResolver;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.providers.BoxProvider;
import de.qabel.qabelbox.providers.MockBoxProvider;
import de.qabel.qabelbox.storage.BoxVolume;
import org.junit.Before;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Jan D.S. Wischweh <mail@wischweh.de> on 07.03.16.
 */
public abstract class MockedBoxProviderTest extends InstrumentationTestCase {

    MockBoxProvider mockProvider;
    protected MockContentResolver mockContentResolver;
    private BoxVolume volume;
    Activity activity;
    public static String ROOT_DOC_ID;


    public abstract Context getContext();

    @Before
    public void setUp() throws Exception {
        configureTestServer();
        initMockContext();
        initRootVolume();

    }

    protected void configureTestServer() {

        new AppPreference(getContext())
            .setToken(getContext().getString(R.string.blockserver_magic_testtoken));
        URLs.setBaseBlockURL(getContext().getString(R.string.testBlockServer));


    }

    private void initMockContext() {
        mockProvider = new MockBoxProvider();
        mockProvider.mockBindToService(getContext());
        mockContentResolver = new MockContentResolver();
        mockContentResolver.addProvider(BoxProvider.AUTHORITY, mockProvider);

    }

    private void initRootVolume() throws QblStorageException {
        byte[] deviceID = getProvider().deviceID;
        MockBoxProvider provider = getProvider();
        ROOT_DOC_ID = provider.rootDocId;
        volume = new BoxVolume(provider.keyPair, provider.prefix, deviceID, getContext());
        volume.createIndex();

    }

    protected MockBoxProvider getProvider() {
        return mockProvider;
    }

    public BoxVolume getVolume() {
        return volume;
    }

    public void injectFile(String filename, String content) {

    }


    private void writeFileContent(Uri uri) {
        try {
            ParcelFileDescriptor pfd =
                activity.getContentResolver().
                    openFileDescriptor(uri, "w");

            FileOutputStream fileOutputStream =
                new FileOutputStream(pfd.getFileDescriptor());

            String textContent = "";

            fileOutputStream.write(textContent.getBytes());

            fileOutputStream.close();
            pfd.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
