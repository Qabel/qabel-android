package de.qabel.qabelbox.ui;

import android.os.PowerManager;
import android.support.design.internal.NavigationMenuItemView;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.util.Log;
import com.squareup.spoon.Spoon;
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
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.ui.helper.SystemAnimations;
import de.qabel.qabelbox.ui.helper.UIActionHelper;
import de.qabel.qabelbox.ui.helper.UIBoxHelper;
import de.qabel.qabelbox.ui.helper.UITestHelper;
import de.qabel.qabelbox.ui.matcher.QabelMatcher;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.openDrawer;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.test.MoreAsserts.assertNotEmpty;
import static junit.framework.Assert.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.*;

public class ChatMessageUITest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class, false, true);
    private MainActivity mActivity;
    private UIBoxHelper mBoxHelper;
    private PowerManager.WakeLock wakeLock;
    private SystemAnimations mSystemAnimations;
    private final String TAG = getClass().getSimpleName();
    private Contact contact2, contact1;

    public ChatMessageUITest() throws IOException {
        //setup data before MainActivity launched. This avoid the call to create identity
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
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        wakeLock = UIActionHelper.wakeupDevice(mActivity);
        mSystemAnimations = new SystemAnimations(mActivity);
        mSystemAnimations.disableAll();
    }

    @Test
    public void testSendMessage() {
        Spoon.screenshot(mActivity, "empty");
        sendOneAndCheck(1);
        sendOneAndCheck(2);
    }

    /**
     * test visualization of chatmessage. messages direct injected with ui
     */
    @Test
    public void testNewMessageVisualization() {

        //prepaire data
        LocalQabelService service = QabelBoxApplication.getInstance().getService();

        String identityKey = service.getActiveIdentity().getEcPublicKey().getReadableKeyIdentifier();
        contact1 = createContact("contact1");
        contact2 = createContact("contact2");
        ChatServer chatServer = mActivity.chatServer;
        String contact1Alias = contact1.getAlias();
        String contact2Alias = contact2.getAlias();

        String contact1Key = contact1.getEcPublicKey().getReadableKeyIdentifier();
        String contact2Key = contact2.getEcPublicKey().getReadableKeyIdentifier();

        //start test... go to contact fragment
        openDrawer(R.id.drawer_layout);
        onView(allOf(withText(R.string.Contacts), withParent(withClassName(endsWith("MenuView")))))
                .perform(click());
        Spoon.screenshot(mActivity, "contacts");
        int messageCount = chatServer.getAllMessages(contact1).length;
        Log.d(TAG, "count: " + messageCount);

        addMessageFromOneContact(identityKey, chatServer, contact1Alias, contact1Key);
        addMessageFromTwoContacts(identityKey, chatServer, contact1Alias, contact2Alias, contact1Key, contact2Key);
        addOwnMessage(chatServer, contact1Alias, contact2Alias, contact1Key);

    }

    private void setupData() {
        URLs.setBaseBlockURL(TestConstants.BLOCK_URL);
        mActivity = mActivityTestRule.getActivity();
        mBoxHelper = new UIBoxHelper(QabelBoxApplication.getInstance());
        mBoxHelper.bindService(QabelBoxApplication.getInstance());
        mBoxHelper.createTokenIfNeeded(false);
        try {
            Identity old = mBoxHelper.getCurrentIdentity();
            if (old != null) {
                mBoxHelper.deleteIdentity(old);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mBoxHelper.removeAllIdentities();
        Identity user1 = mBoxHelper.addIdentity("user1");
        Identity user2 = mBoxHelper.addIdentity("user2");
        String contact1Json = ContactExportImport.exportIdentityAsContact(user1);
        String contact2Json = ContactExportImport.exportIdentityAsContact(user2);
        mBoxHelper.setActiveIdentity(user1);
        addContact(user1, contact2Json);
        assertNotEmpty(mBoxHelper.getService().getContacts().getContacts());
        mBoxHelper.setActiveIdentity(user2);
        addContact(user2, contact1Json);


        assertNotEmpty(mBoxHelper.getService().getContacts().getContacts());
    }

    private void addContact(Identity identity, String contact) {
        try {
            mBoxHelper.getService().addContact(ContactExportImport.parseContactForIdentity(identity, new JSONObject(contact)));
        } catch (Exception e) {
            Log.e(TAG, "error on add contact", e);
        }
    }

    private void sendOneAndCheck(int messages) {
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
        onView(withId(R.id.etText)).perform(typeText("text" + messages), pressImeActionButton());
        closeSoftKeyboard();
        onView(withText(R.string.btn_chat_send)).check(matches(isDisplayed())).perform(click());

        UITestHelper.sleep(200);

        onView(withId(R.id.contact_chat_list)).
                check(matches(isDisplayed())).
                check(matches(QabelMatcher.withListSize(messages)));
        pressBack();

        //go to identity user 1
        openDrawer(R.id.drawer_layout);
        onView(withId(R.id.imageViewExpandIdentity)).check(matches(isDisplayed())).perform(click());
        onView(allOf(is(instanceOf(NavigationMenuItemView.class)), withText("user1"))).perform(click());

        openDrawer(R.id.drawer_layout);
        onView(withText(R.string.Contacts)).check(matches(isDisplayed())).perform(click());
        Spoon.screenshot(mActivity, "message" + messages);
        checkVisibilityState("user2", QabelMatcher.isVisible());
        onView(withText("user2")).check(matches(isDisplayed())).perform(click());

        onView(withId(R.id.contact_chat_list)).
                check(matches(isDisplayed())).
                check(matches(QabelMatcher.withListSize(messages)));
        pressBack();

        //go to user 2
        openDrawer(R.id.drawer_layout);
        onView(withId(R.id.imageViewExpandIdentity)).check(matches(isDisplayed())).perform(click());
        onView(allOf(is(instanceOf(NavigationMenuItemView.class)), withText("user2"))).perform(click());
        openDrawer(R.id.drawer_layout);
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


    private void addMessageFromTwoContacts(String identityKey, ChatServer chatServer, String contact1Alias, String contact2Alias, String contact1Key, String contact2Key) {
        ChatMessageItem dbItem;
        int newMessageCount;

        //send message to 2 users
        dbItem = createNewChatMessageItem(contact1Key, identityKey, "from: " + contact1Alias + "message2");
        chatServer.storeIntoDB(dbItem);
        dbItem = createNewChatMessageItem(contact2Key, identityKey, "from: " + contact2Alias + "message1");
        chatServer.storeIntoDB(dbItem);


        //check if 2 contacts have indicator abd click on contact1
        refreshContactView(chatServer);
        Spoon.screenshot(mActivity, "contactsTwoNewMessage");

        //check if complete db match correct entry size
        assertThat(chatServer.getAllMessages().length, is(3));

        //check states
        assertTrue(chatServer.hasNewMessages(contact1));
        assertTrue(chatServer.hasNewMessages(contact2));

        //check single entry count
        newMessageCount = chatServer.getAllMessages(contact1).length + chatServer.getAllMessages(contact2).length;
        assertThat(newMessageCount, is(3));

        //check indicator visibility
        checkVisibilityState(contact2Alias, QabelMatcher.isVisible());
        checkVisibilityState(contact1Alias, QabelMatcher.isVisible()).perform(click());

        //check RecyclerView size
        onView(withId(R.id.contact_chat_list)).check(matches(QabelMatcher.withListSize(2)));
        pressBack();

        //check if indicator on contact 1 not displayer
        checkVisibilityState(contact1Alias, QabelMatcher.isInvisible());

        //same with contact2
        checkVisibilityState(contact2Alias, QabelMatcher.isVisible()).perform(click());
        onView(withId(R.id.contact_chat_list)).check(matches(QabelMatcher.withListSize(1)));
        pressBack();
        checkVisibilityState(contact2Alias, QabelMatcher.isInvisible());
    }

    private void addMessageFromOneContact(String identityKey, ChatServer chatServer, String contact1Alias, String contact1Key) {
        ChatMessageItem dbItem = createNewChatMessageItem(contact1Key, identityKey, "from: " + contact1Alias + "message1");
        chatServer.storeIntoDB(dbItem);
        int newMessageCount = chatServer.getAllMessages(contact1).length;

        Spoon.screenshot(mActivity, "contactsOneNewMessage");
        assertThat(1, is(newMessageCount));
        assertTrue(chatServer.hasNewMessages(contact1));
        assertFalse(chatServer.hasNewMessages(contact2));

        //check if new view indicator displayed on correct user and click on this item
        refreshContactView(chatServer);
        checkVisibilityState(contact1Alias, QabelMatcher.isVisible()).perform(click());

        //check if RecyclerView contain correct count of data
        onView(withId(R.id.contact_chat_list)).check(matches(QabelMatcher.withListSize(1)));
        pressBack();
        //check if indicator not displayer (we have viewed the item)
        checkVisibilityState(contact1Alias, QabelMatcher.isInvisible());
    }

    private void addOwnMessage(ChatServer chatServer, String contact1Alias, String contact2Alias, String contact1Key) {
        ChatMessageItem item = new ChatMessageItem(chatServer.getTextDropMessage("ownmessage"));
        item.receiver = contact1Key;
        item.sender = QabelBoxApplication.getInstance().getService().getActiveIdentity().getEcPublicKey().getReadableKeyIdentifier();
        item.isNew = 0;

        chatServer.storeIntoDB(item);
        refreshContactView(chatServer);
        checkVisibilityState(contact2Alias, QabelMatcher.isInvisible());
        onView(withText(contact1Alias)).perform(click());
        onView(withId(R.id.contact_chat_list)).check(matches(QabelMatcher.withListSize(3)));
        pressBack();
    }

    /**
     * check if new message indicator displayed
     *
     * @param alias      contact name
     * @param visibility viewAssertion eg visibile or inVisible
     * @return ViewInteraction
     */
    private ViewInteraction checkVisibilityState(String alias, ViewAssertion visibility) {
        return onView(allOf(QabelMatcher.withDrawable(R.drawable.ic_visibility), hasSibling(withText(alias)))).check(visibility);
    }

    private void refreshContactView(ChatServer chatServer) {
        chatServer.sendCallbacksRefreshed();
        UITestHelper.sleep(500);
    }

    private ChatMessageItem createNewChatMessageItem(String sender, String receiver, String message) {
        return new ChatMessageItem(-1, (short) 1, System.currentTimeMillis() + System.nanoTime() % 1000, sender, receiver, message, ChatMessageItem.BOX_MESSAGE, mActivity.chatServer.getTextDropMessagePayload(message));
    }

}

