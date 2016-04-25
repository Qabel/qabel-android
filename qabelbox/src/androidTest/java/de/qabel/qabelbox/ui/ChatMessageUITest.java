package de.qabel.qabelbox.ui;

import android.content.Intent;
import android.os.PowerManager;
import android.support.design.internal.NavigationMenuItemView;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.util.Log;

import com.squareup.spoon.Spoon;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.TestConstants;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.chat.ChatMessageItem;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.communication.URLs;
import de.qabel.qabelbox.config.ContactExportImport;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.helper.AccountHelper;
import de.qabel.qabelbox.helper.Helper;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import de.qabel.qabelbox.ui.matcher.QabelMatcher;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.openDrawer;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.test.MoreAsserts.assertNotEmpty;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

public class ChatMessageUITest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<MainActivity>(MainActivity.class, false, true) {
        @Override
        public void beforeActivityLaunched() {
            setupData();
        }

    };
    private MainActivity mActivity;
    private UIBoxHelper mBoxHelper;
    private PowerManager.WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;
    private final String TAG = this.getClass().getSimpleName();
    private Contact contact2, contact1;
    private Identity identity;

    @After
    public void cleanUp() {
        wakeLock.release();
        mSystemAnimations.enableAll();
        mBoxHelper.unbindService(QabelBoxApplication.getInstance());
    }

    @Before
    public void setUp() throws IOException, QblStorageException {

        mActivity = mActivityTestRule.getActivity();
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
        LocalQabelService service = QabelBoxApplication.getInstance().getService();
        identity = service.getActiveIdentity();
        AccountHelper.createSyncAccount(mActivity);
    }
    @Test
    public void testSendOneMessage() {
        Spoon.screenshot(mActivity, "empty");
        openDrawer(R.id.drawer_layout);

        onView(allOf(withText(R.string.Contacts), withParent(withClassName(endsWith("MenuView")))))
                .perform(click());
        Spoon.screenshot(mActivity, "contacts");

        //ContactList and click on user
        onView(withId(R.id.contact_list)).check(matches(isDisplayed()));
        onView(withText("user1")).perform(click());

        //ChatView is displayed
        onView(withId(R.id.contact_chat_list)).check(matches(isDisplayed()));

        //Check Username is displayed in chatview
        QabelMatcher.matchToolbarTitle("user1").check(matches(isDisplayed()));

        onView(withId(R.id.etText)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.etText)).perform(typeText("text" + 1), pressImeActionButton());
        closeSoftKeyboard();
        onView(withText(R.string.btn_chat_send)).check(matches(isDisplayed())).perform(click());

        onView(withId(R.id.contact_chat_list)).
                check(matches(isDisplayed())).
                check(matches(QabelMatcher.withListSize(1)));
        pressBack();

        //go to identity user 1
        openDrawer(R.id.drawer_layout);
        onView(withId(R.id.imageViewExpandIdentity)).check(matches(isDisplayed())).perform(click());
        onView(allOf(is(instanceOf(NavigationMenuItemView.class)), withText("user1"))).perform(click());

        openDrawer(R.id.drawer_layout);
        onView(withText(R.string.Contacts)).check(matches(isDisplayed())).perform(click());
        Spoon.screenshot(mActivity, "message" + 1);
        checkVisibilityState("user2", QabelMatcher.isVisible());
        onView(withText("user2")).check(matches(isDisplayed())).perform(click());

        onView(withId(R.id.contact_chat_list)).
                check(matches(isDisplayed())).
                check(matches(QabelMatcher.withListSize(1)));
        pressBack();

        //go to user 2
        openDrawer(R.id.drawer_layout);
        onView(withId(R.id.imageViewExpandIdentity)).check(matches(isDisplayed())).perform(click());
        onView(allOf(is(instanceOf(NavigationMenuItemView.class)), withText("user2"))).perform(click());
        openDrawer(R.id.drawer_layout);
    }

    /**
     * test visualization of chatmessage. messages direct injected with ui
     */
    @Test
    public void testNewMessageVisualization() {

        String identityKey = identity.getEcPublicKey().getReadableKeyIdentifier();
        contact1 = createContact("contact1");
        contact2 = createContact("contact2");
        ChatServer chatServer = mActivity.chatServer;
        String contact1Alias = contact1.getAlias();

        String contact1Key = contact1.getEcPublicKey().getReadableKeyIdentifier();

        //start test... go to contact fragment
        openDrawer(R.id.drawer_layout);
        onView(allOf(withText(R.string.Contacts), withParent(withClassName(endsWith("MenuView")))))
                .perform(click());
        Spoon.screenshot(mActivity, "contacts");
        int messageCount = chatServer.getAllMessages(identity, contact1).length;
        Log.d(TAG, "count: " + messageCount);


        ChatMessageItem dbItem = createNewChatMessageItem(contact1Key, identityKey, "from: " + contact1Alias + "message1");
        chatServer.storeIntoDB(identity, dbItem);
        int newMessageCount = chatServer.getAllMessages(identity, contact1).length;

        Spoon.screenshot(mActivity, "contactsOneNewMessage");
        assertThat(1, is(newMessageCount));
        assertTrue(chatServer.hasNewMessages(identity, contact1));
        assertFalse(chatServer.hasNewMessages(identity, contact2));

        //check if new view indicator displayed on correct user and click on this item
        refreshContactView();
        checkVisibilityState(contact1Alias, QabelMatcher.isVisible()).perform(click());

        //check if RecyclerView contain correct count of data
        onView(withId(R.id.contact_chat_list)).check(matches(QabelMatcher.withListSize(1)));
        pressBack();
        refreshContactView();
        UITestHelper.sleep(500);
        //check if indicator not displayed (we have viewed the item)
        checkVisibilityState(contact1Alias, QabelMatcher.isInvisible());

    }

    private void setupData() {
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        mActivity = mActivityTestRule.getActivity();
        mBoxHelper = new UIBoxHelper(QabelBoxApplication.getInstance());
        mBoxHelper.bindService(QabelBoxApplication.getInstance());
        mBoxHelper.createTokenIfNeeded(false);
        mBoxHelper.removeAllIdentities();
        Identity user1 = mBoxHelper.addIdentityWithoutVolume("user1");
        Identity user2 = mBoxHelper.addIdentityWithoutVolume("user2");

        addContact(user1, user2);
        addContact(user2, user1);
        mBoxHelper.setActiveIdentity(user2);


        assertNotEmpty(mBoxHelper.getService().getContacts().getContacts());
    }

    private void addContact(Identity identity, Identity contact) {
        Contact asContact = new Contact(contact.getAlias(),
                contact.getDropUrls(),contact.getEcPublicKey());
		mBoxHelper.getService().addContact(asContact, identity);
    }

    private Contact createContact(String name) {
        Identity identity = mBoxHelper.createIdentity(name);
        String json = ContactExportImport.exportIdentityAsContact(identity);
        return addContact(json);
    }

    private Contact addContact(String contactJSON) {
        try {
            Contact contact = ContactExportImport.parseContactForIdentity(null, new JSONObject(contactJSON));
            mBoxHelper.getService().addContact(contact);
            return contact;
        } catch (Exception e) {
            assertNotNull(e);
            Log.e(TAG, "error on add contact", e);
        }
        return null;
    }


    /**
     * check if new message indicator displayed
     *
     * @param alias      contact name
     * @param visibility viewAssertion eg visibile or inVisible
     * @return ViewInteraction
     */
    private ViewInteraction checkVisibilityState(String alias, ViewAssertion visibility) {
        UITestHelper.sleep(500);
        return onView(allOf(QabelMatcher.withDrawable(R.drawable.eye), hasSibling(withText(alias)))).check(visibility);
    }

    private void refreshContactView() {
        AccountHelper.startOnDemandSyncAdapter(mActivity);
        Intent intent = new Intent(Helper.INTENT_REFRESH_CONTACTLIST);
        mActivity.getApplicationContext().sendBroadcast(intent);
        intent = new Intent(Helper.INTENT_REFRESH_CHAT);
        mActivity.getApplicationContext().sendBroadcast(intent);
    }

    private ChatMessageItem createNewChatMessageItem(String sender, String receiver, String message) {
        return new ChatMessageItem(-1, (short) 1, System.currentTimeMillis() + System.nanoTime() % 1000, sender, receiver, message, ChatMessageItem.BOX_MESSAGE, mActivity.chatServer.createTextDropMessagePayload(message));
    }

}

