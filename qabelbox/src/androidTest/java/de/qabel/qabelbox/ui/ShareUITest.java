package de.qabel.qabelbox.ui;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.rule.IntentsTestRule;

import org.hamcrest.core.AllOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class ShareUITest {
    @Rule
    public IntentsTestRule<MainActivity> mActivityTestRule =
            new MainActivityWithoutFilesFragmentTestRule();
    private MainActivity mActivity;
    private UIBoxHelper mBoxHelper;
    private PowerManager.WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;


    @After
    public void cleanUp() {
        wakeLock.release();
        mSystemAnimations.enableAll();
    }

    @Before
    public void setUp() throws Exception {
        setupData();
        mActivity = mActivityTestRule.launchActivity(null);
        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
    }

    private void setupData() throws Exception {
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        Context context = InstrumentationRegistry.getTargetContext();
        UITestHelper.disableBugReporting(context);
        mActivity = mActivityTestRule.getActivity();
        mBoxHelper = new UIBoxHelper(context);
        mBoxHelper.createTokenIfNeeded(false);
        mBoxHelper.addIdentity("share");
    }


    @Ignore("Drawer layout rebuild")
    @Test
    public void testTellAFriend() {
        //openDrawer(R.id.drawer_layout);
        Intents.intending(AllOf.allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(Intent.EXTRA_TITLE, mActivity.getString(R.string.share_via))
        )).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));

        onView(withText(R.string.action_tellafriend)).perform(click());
        intended(allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(Intent.EXTRA_TITLE, mActivity.getString(R.string.share_via))));

    }


}
