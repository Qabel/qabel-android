package de.qabel.qabelbox.ui;

import android.content.Intent;

import org.junit.Test;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.openDrawer;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class MainActivityTest extends AbstractUITest {

    @Test
    public void testChangeActiveIdentity() throws Throwable {
        Identity second = mBoxHelper.addIdentity("second");
        startWithIdentity(identity);
        openDrawer(R.id.drawer_layout);
        onView(withId(R.id.imageViewExpandIdentity)).check(matches(isDisplayed())).perform(click());
        onView(withText("second")).perform(click());
        intended(hasExtra(MainActivity.ACTIVE_IDENTITY, second.getKeyIdentifier()));
    }

    @Test
    public void testStartWithIdentity() throws Throwable {
        Identity second = mBoxHelper.addIdentity("second");
        startWithIdentity(second);

        assertThat(mActivity.getActiveIdentity().getKeyIdentifier(),
                equalTo(second.getKeyIdentifier()));
    }

    public void startWithIdentity(Identity identity) {
        Intent intent = new Intent(mContext, MainActivity.class);
        intent.putExtra(MainActivity.ACTIVE_IDENTITY, identity.getKeyIdentifier());
        intent.putExtra(MainActivity.START_FILES_FRAGMENT, false);
        launchActivity(intent);
    }
}
