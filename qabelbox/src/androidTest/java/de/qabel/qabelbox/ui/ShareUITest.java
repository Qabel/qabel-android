package de.qabel.qabelbox.ui;

import android.app.Activity;
import android.app.Instrumentation.ActivityResult;
import android.content.Intent;
import android.os.PowerManager.WakeLock;
import android.support.design.internal.NavigationMenuItemView;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R.id;
import de.qabel.qabelbox.R.string;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import org.hamcrest.core.AllOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.contrib.DrawerActions.openDrawer;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

public class ShareUITest {
    @Rule
    public IntentsTestRule<MainActivity> mActivityTestRule = new IntentsTestRule<>(MainActivity.class, false, true);
    private MainActivity mActivity;
    private UIBoxHelper mBoxHelper;
    private WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;


    public ShareUITest() throws IOException {
        setupData();
    }

    @After
    public void cleanUp() {
        wakeLock.release();
        mSystemAnimations.enableAll();
        mBoxHelper.unbindService(QabelBoxApplication.getInstance());
    }

    @Before
    public void setUp() throws IOException, QblStorageException {
        mActivity = mActivityTestRule.getActivity();
        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
    }

    private void setupData() {
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        UITestHelper.disableBugReporting(QabelBoxApplication.getInstance().getApplicationContext());
        mActivity = mActivityTestRule.getActivity();
        mBoxHelper = new UIBoxHelper(QabelBoxApplication.getInstance());
        mBoxHelper.bindService(QabelBoxApplication.getInstance());
        mBoxHelper.createTokenIfNeeded(false);
        mBoxHelper.addIdentity("share");
    }


    @Test
    public void testTellAFriend() {
        openDrawer(id.drawer_layout);
        Intents.intending(AllOf.allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(Intent.EXTRA_TITLE, mActivity.getString(string.share_via))
        )).respondWith(new ActivityResult(Activity.RESULT_OK, null));

        onView(allOf(is(instanceOf(NavigationMenuItemView.class)), withText(string.action_tellafriend))).perform(click());
        intended(allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(Intent.EXTRA_TITLE, mActivity.getString(string.share_via))));

    }


}
