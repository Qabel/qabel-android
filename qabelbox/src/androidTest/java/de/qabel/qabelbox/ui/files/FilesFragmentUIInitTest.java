package de.qabel.qabelbox.ui.files;

import android.content.Intent;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.storage.BoxVolume;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

public class FilesFragmentUIInitTest extends FilesFragmentUITestBase {

    private Identity testIdentity2;

    @Before
    public void setUp() throws Throwable {
        this.autoLaunch = false;
        super.setUp();
    }

    private void launchActivity(String identity) {
        Intent intent = new Intent(mContext, MainActivity.class);
        intent.putExtra(MainActivity.ACTIVE_IDENTITY, identity);
        launchActivity(intent);
        mActivity.filesFragment.injectIdleCallback(getIdlingResource());
    }

    @Override
    protected void setupData() throws Exception {
        testIdentity2 = mBoxHelper.addIdentityWithoutVolume("spoon2");
        mBoxHelper.setActiveIdentity(testIdentity2);
    }

    @Test
    public void testCreateInitialVolume() throws Exception {
        launchActivity(testIdentity2.getKeyIdentifier());
        assertNotNull("Navigation not initialized!", mActivity.filesFragment.getBoxNavigation());
    }

    @Test
    public void testVolumeNotAvailable() throws Exception {
        //Corrupt volume root file
        BoxVolume volume = mBoxHelper.getIdentityVolume(identity);
        byte[] random = RandomUtils.nextBytes(1000);
        mBoxHelper.getBoxManager().blockingUpload(identity.getPrefixes().get(0),
                volume.getRootRef(), new ByteArrayInputStream(random));

        //Start with corrupted volume
        launchActivity(identity.getKeyIdentifier());
        //if a navigation is created, the existing volume has been overridden.
        assertNull("Navigation initialized, overriding volume", mActivity.filesFragment.getBoxNavigation());
    }

}
