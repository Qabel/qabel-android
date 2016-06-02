package de.qabel.qabelbox.ui;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import de.qabel.core.config.Identity;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.repository.sqlite.AndroidClientDatabase;
import de.qabel.desktop.repository.sqlite.SqliteContactRepository;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.persistence.RepositoryFactory;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;

public class AbstractUITest {
    @Rule
    public IntentsTestRule<MainActivity> mActivityTestRule =
            new MainActivityWithoutFilesFragmentTestRule(false);
    protected MainActivity mActivity;
    protected UIBoxHelper mBoxHelper;
    protected Identity identity;
    private PowerManager.WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;
    protected Context mContext;
    protected IdentityRepository identityRepository;
    protected ContactRepository contactRepository;

    @After
    public void cleanUp() {
        if (wakeLock != null) {
            wakeLock.release();
        }
        if (mSystemAnimations != null) {
            mSystemAnimations.enableAll();
        }
    }

    @Before
    public void setUp() throws Throwable {
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        mContext = InstrumentationRegistry.getTargetContext();

        mBoxHelper = new UIBoxHelper(mContext);
        mBoxHelper.createTokenIfNeeded(false);
        mBoxHelper.removeAllIdentities();
        identity = mBoxHelper.addIdentity("spoon123");
        identityRepository = mBoxHelper.getIdentityRepository();
        contactRepository = mBoxHelper.getContactRepository();

    }

    protected void launchActivity(@Nullable Intent intent) {
        if (intent == null) {
            intent = new Intent(mContext, MainActivity.class);
            intent.putExtra(MainActivity.START_FILES_FRAGMENT, false);
        }
        if(!intent.hasExtra(MainActivity.ACTIVE_IDENTITY)){
            intent.putExtra(MainActivity.ACTIVE_IDENTITY, identity.getKeyIdentifier());
        }
        mActivity = mActivityTestRule.launchActivity(intent);
        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
    }
}
