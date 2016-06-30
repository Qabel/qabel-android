package de.qabel.qabelbox.ui;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.chat.ChatMessageItem;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.fragments.ContactFragment;
import de.qabel.qabelbox.helper.Helper;
import de.qabel.qabelbox.navigation.MainNavigator;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import de.qabel.qabelbox.ui.idling.InjectedIdlingResource;
import de.qabel.qabelbox.ui.matcher.QabelMatcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class ChatMessageUITest extends AbstractUITest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule =
            new MainActivityWithoutFilesFragmentTestRule(false);
    private final String TAG = this.getClass().getSimpleName();
    private Contact contact;

    public ChatMessageUITest() {
    }

    @Before
    public void setUp() throws Throwable {
        super.setUp();
        Identity user2 = mBoxHelper.addIdentityWithoutVolume("user2");
        contact = addContact(identity, user2);

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.putExtra(MainActivity.START_CONTACTS_FRAGMENT, true);
        intent.putExtra(MainActivity.ACTIVE_IDENTITY, identity.getKeyIdentifier());
        launchActivity(intent);
    }

    /**
     * test visualization of chatmessage. messages direct injected with ui
     */
    @Test
    public void testNewMessageVisualization() throws Throwable {
        InjectedIdlingResource idlingResource = new InjectedIdlingResource();
        ContactFragment fragment = (ContactFragment) mActivity.getFragmentManager()
                .findFragmentByTag(MainNavigator.TAG_CONTACT_LIST_FRAGMENT);
        fragment.setIdleCallback(idlingResource);

        Context context = InstrumentationRegistry.getTargetContext();
        String identityKey = identity.getEcPublicKey().getReadableKeyIdentifier();
        ChatServer chatServer = new ChatServer(context);
        String contact1Alias = contact.getAlias();

        String contact1Key = contact.getEcPublicKey().getReadableKeyIdentifier();

        ChatMessageItem dbItem = createNewChatMessageItem(contact1Key, identityKey,
                "from: " + contact1Alias + "message1");
        chatServer.storeIntoDB(identity, dbItem);
        UITestHelper.screenShot(mActivity, "contactsOneNewMessage");
        refreshViewIntent(context);
        idlingResource.busy();

        //check if new view indicator displayed on correct user and click on this item
        checkVisibilityState(contact1Alias, QabelMatcher.isVisible()).perform(click());

        //check if RecyclerView contain correct count of data
        onView(withId(R.id.contact_chat_list)).check(matches(QabelMatcher.withListSize(1)));
        pressBack();
        //check if indicator not displayed (we have viewed the item)
        checkVisibilityState(contact1Alias, QabelMatcher.isInvisible());

    }

    private Contact addContact(Identity identity, Identity contact)
            throws PersistenceException {
        Contact asContact = new Contact(contact.getAlias(),
                contact.getDropUrls(),contact.getEcPublicKey());
        contactRepository.save(asContact, identity);
        return asContact;
    }

    private void refreshViewIntent(Context context) {
        Intent intent = new Intent(Helper.INTENT_REFRESH_CONTACTLIST);
        context.sendBroadcast(intent, null);
    }

    /**
     * check if new message indicator displayed
     *
     * @param alias      contact name
     * @param visibility viewAssertion eg visibile or inVisible
     * @return ViewInteraction
     */
    private ViewInteraction checkVisibilityState(String alias, ViewAssertion visibility) {
        return onView(allOf(QabelMatcher.withDrawable(R.drawable.eye), hasSibling(withText(alias)))).check(visibility);
    }

    private ChatMessageItem createNewChatMessageItem(String sender, String receiver, String message) {
        return new ChatMessageItem(-1, (short) 1, System.currentTimeMillis() + System.nanoTime() % 1000, sender, receiver, message, ChatMessageItem.BOX_MESSAGE, ChatServer.createTextDropMessagePayload(message));
    }

}

