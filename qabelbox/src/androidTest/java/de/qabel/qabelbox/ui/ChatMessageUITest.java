package de.qabel.qabelbox.ui;

/**
 * Created by danny on 05.01.2016.
 */

import android.os.PowerManager;
import android.support.design.internal.NavigationMenuItemView;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.util.Log;

import com.squareup.spoon.Spoon;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

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

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.openDrawer;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.test.MoreAsserts.assertNotEmpty;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
//import static de.qabel.qabelbox.ui.matcher.QabelMatcher.withDrawable;

/**
 * Tests for MainActivity.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ChatMessageUITest {

	@Rule
	public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class, false, true);
	private MainActivity mActivity;
	private UIBoxHelper mBoxHelper;
	private final boolean mFillAccount = true;
	private PowerManager.WakeLock wakeLock;
	private SystemAnimations mSystemAnimations;
	private Identity user1, user2;
	private String contact1Json, contact2Json;
	private String TAG = this.getClass().getSimpleName();
	private Contact contact2, contact1;

	public ChatMessageUITest() throws IOException {
		//setup data before MainActivity launched. This avoid the call to create identity
		if (mFillAccount) {
			setupData();
		}
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
		user1 = mBoxHelper.addIdentity("user1");
		user2 = mBoxHelper.addIdentity("user2");
		contact1Json = ContactExportImport.exportIdentityAsContact(user1);
		contact2Json = ContactExportImport.exportIdentityAsContact(user2);
		mBoxHelper.setActiveIdentity(user1);
		try {
			mBoxHelper.getService().addContact(new ContactExportImport().parseContactForIdentity(user1, new JSONObject(contact2Json)));
		} catch (Exception e) {
			Log.e(TAG, "error on add contact", e);
		}
		assertNotEmpty(mBoxHelper.getService().getContacts().getContacts());
		mBoxHelper.setActiveIdentity(user2);
		try {
			mBoxHelper.getService().addContact(new ContactExportImport().parseContactForIdentity(user2, new JSONObject(contact1Json)));
		} catch (Exception e) {
			Log.e(TAG, "error on add contact", e);
		}
		assertNotEmpty(mBoxHelper.getService().getContacts().getContacts());
	}


/*
	@Test
    public void testSendMessage() {
        Spoon.screenshot(mActivity, "empty");
        sendOneAndCheck(1);
        sendOneAndCheck(2);
    }*/

	protected void sendOneAndCheck(int messages) {
		openDrawer(R.id.drawer_layout);

		onView(allOf(withText(R.string.Contacts), withParent(withClassName(endsWith("MenuView")))))
				.perform(click());
		Spoon.screenshot(mActivity, "contacts");

		onView(withId(R.id.contact_list))
				.perform(RecyclerViewActions.actionOnItem(
						hasDescendant(withText("user1")), click()));

		onView(withId(R.id.etText)).check(matches(isDisplayed())).perform(click());
		onView(withId(R.id.etText)).perform(typeText("text" + messages), pressImeActionButton());
		closeSoftKeyboard();
		onView(withText(R.string.btn_chat_send)).check(matches(isDisplayed())).perform(click());
		UITestHelper.sleep(1000);

		onView(withId(R.id.contact_chat_list)).
				check(matches(isDisplayed())).
				check(matches(QabelMatcher.withListSize(messages)));
		pressBack();

		//go to identity user 1
		openDrawer(R.id.drawer_layout);
		onView(withId(R.id.imageViewExpandIdentity)).check(matches(isDisplayed())).perform(click());
		UITestHelper.sleep(500);
		onView(allOf(is(instanceOf(NavigationMenuItemView.class)), withText("user1"))).perform(click());

		openDrawer(R.id.drawer_layout);
		onView(withText(R.string.Contacts)).check(matches(isDisplayed())).perform(click());
		Spoon.screenshot(mActivity, "message" + messages);

		onView(withText("user2")).check(matches(isDisplayed())).perform(click());
		onView(withId(R.id.contact_chat_list)).
				check(matches(isDisplayed())).
				check(matches(QabelMatcher.withListSize(messages)));
		pressBack();

		//go to user 2
		openDrawer(R.id.drawer_layout);
		onView(withId(R.id.imageViewExpandIdentity)).check(matches(isDisplayed())).perform(click());
		UITestHelper.sleep(500);
		onView(allOf(is(instanceOf(NavigationMenuItemView.class)), withText("user2"))).perform(click());
		openDrawer(R.id.drawer_layout);
	}

	/**
	 * test visualization of chatmessage. messages direct injected with ui
	 */
	@Test
	public void testNewMessageVisualization() {

		//add second contact
		LocalQabelService service = QabelBoxApplication.getInstance().getService();
		try {
			mBoxHelper.getService().addContact(new ContactExportImport().parseContactForIdentity(user1, new JSONObject(contact2Json)));
		} catch (Exception e) {
			e.printStackTrace();
			assertNull(e);
		}

		//prepaire data
		String identityKey = service.getActiveIdentity().getEcPublicKey().getReadableKeyIdentifier();

		Set<Contact> contacts = service.getContacts().getContacts();
		ChatServer chatServer = mActivity.chatServer;
		Iterator<Contact> it = contacts.iterator();
		contact1 = it.next();
		contact2 = it.next();
		String contact1Alias = contact1.getAlias();
		String contact2Alias = contact2.getAlias();

		String contact1Key = contact1.getEcPublicKey().getReadableKeyIdentifier();
		String contact2Key = contact2.getEcPublicKey().getReadableKeyIdentifier();

		//start test
		openDrawer(R.id.drawer_layout);
		onView(allOf(withText(R.string.Contacts), withParent(withClassName(endsWith("MenuView")))))
				.perform(click());
		Spoon.screenshot(mActivity, "contacts");
		int messageCount = chatServer.getAllMessages(contact1).length;
		Log.d(TAG, "count: " + messageCount);

		//add new message
		ChatMessageItem dbItem = createNewChatMessageItem(contact1Key, identityKey, "to: " + contact1Alias + "message1");
		chatServer.storeIntoDB(dbItem);
		int newMessageCount = chatServer.getAllMessages(contact1).length;

		Spoon.screenshot(mActivity, "contactsOneNewMessage");
		assertThat(1, is(newMessageCount));

		//check if new view indicator displayed on correct user and click on this item
		refreshContactView(chatServer);
		checkVisibilityState(contact1Alias, QabelMatcher.isVisible()).perform(click());

		//check if recyclerview contain correct count of data
		onView(withId(R.id.contact_chat_list)).check(matches(QabelMatcher.withListSize(1)));
		pressBack();
		//check if indicator not displayer (we have viewer the item)
		checkVisibilityState(contact1Alias, QabelMatcher.isInvisible());


		//send message to 2 users
		dbItem = createNewChatMessageItem(contact1Key, identityKey, "to: " + contact1Alias + "message2");
		chatServer.storeIntoDB(dbItem);
		dbItem = createNewChatMessageItem(contact2Key, identityKey, "to: " + contact2Alias + "message1");
		chatServer.storeIntoDB(dbItem);


		//check if 2 contacts have indicator abd click on contact1
		refreshContactView(chatServer);
		Spoon.screenshot(mActivity, "contactsTwoNewMessage");
		assertThat(chatServer.getAllMessages().length, is(3));

		newMessageCount = chatServer.getAllMessages(contact1).length + chatServer.getAllMessages(contact2).length;
		assertThat(newMessageCount, is(3));


		checkVisibilityState(contact2Alias, QabelMatcher.isVisible());
		checkVisibilityState(contact1Alias, QabelMatcher.isVisible()).perform(click());

		onView(withId(R.id.contact_chat_list)).check(matches(QabelMatcher.withListSize(2)));
		pressBack();
		//check if indicator on contact 1 not displayer
		checkVisibilityState(contact1Alias, QabelMatcher.isInvisible());

		//same with contact2
		checkVisibilityState(contact2Alias, QabelMatcher.isVisible()).perform(click());
		onView(withId(R.id.contact_chat_list)).check(matches(QabelMatcher.withListSize(1)));
		pressBack();
		checkVisibilityState(contact2Alias, QabelMatcher.isInvisible());

/*
		ChatMessageItem item = new ChatMessageItem(identity, publicKey1, "payload2" + i, "payloadtype");
		item.sender = publicKey1;
		item.isNew = 1;
		dataBase.put(item);*/

	}

	private ViewInteraction checkVisibilityState(String alias, ViewAssertion visibility) {
		return onView(allOf(QabelMatcher.withDrawable(R.drawable.ic_visibility), hasSibling(withText(alias)))).check(visibility);
	}

	private void refreshContactView(ChatServer chatServer) {
		//refresh
		chatServer.sendCallbacksRefreshed();
		onView(withId(R.id.action_contact_refresh)).perform(click());
	}

	private ChatMessageItem createNewChatMessageItem(String sender, String receiver, String message) {
		return new ChatMessageItem(-1, (short) 1, System.currentTimeMillis() + System.nanoTime() % 1000, sender, receiver, message, ChatMessageItem.BOX_MESSAGE, mActivity.chatServer.getTextDropMessagePayload(message).toString());
	}
}

