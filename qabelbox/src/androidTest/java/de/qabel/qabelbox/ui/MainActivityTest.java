package de.qabel.qabelbox.ui;

import android.content.Intent;

import org.junit.Test;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.navigation.MainNavigator;
import de.qabel.qabelbox.chat.view.views.ChatFragment;
import de.qabel.qabelbox.util.IdentityHelper;

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
import static org.hamcrest.Matchers.notNullValue;

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

    @Test
    public void testStartWithChat() throws Throwable {
        Contact contact = IdentityHelper.createContact("chat contact");
        contactRepository.save(contact, identity);
        Intent intent = new Intent(mContext, MainActivity.class);
        intent.putExtra(MainActivity.ACTIVE_IDENTITY, identity.getKeyIdentifier());
        intent.putExtra(MainActivity.ACTIVE_CONTACT, contact.getKeyIdentifier());
        intent.putExtra(MainActivity.START_CONTACTS_FRAGMENT, true);
        launchActivity(intent);
        ChatFragment fragment = (ChatFragment) mActivity.getFragmentManager().findFragmentByTag(
                MainNavigator.TAG_CONTACT_CHAT_FRAGMENT);
        assertThat(fragment, notNullValue());
        assertThat(fragment.getContactKeyId(), equalTo(contact.getKeyIdentifier()));
    }
}
