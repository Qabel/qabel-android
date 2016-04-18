package de.qabel.qabelbox.storage;


import android.content.Context;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.RoboApplication;
import de.qabel.qabelbox.chat.ChatMessageItem;
import de.qabel.qabelbox.chat.ChatMessagesDataBase;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.util.IdentityHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = RoboApplication.class, constants = BuildConfig.class)
public class ChatServerTest {

    private Identity identity;
    private Contact contact1;
    private Contact contact2;
    private String publicKey1;
    private String publicKey2;
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.application;
        identity = IdentityHelper.createIdentity("user1", "pre1");

        Identity contactIdentity1 = IdentityHelper.createIdentity("contact1", "per3");
        Identity contactIdentity2 = IdentityHelper.createIdentity("contact1", "pre4");
        contact1 = new Contact("contact1", contactIdentity1.getDropUrls(), contactIdentity1.getEcPublicKey());
        contact2 = new Contact("contact2", contactIdentity2.getDropUrls(), contactIdentity2.getEcPublicKey());
        publicKey1 = getKeyIdentitfier(contact1);
        publicKey2 = getKeyIdentitfier(contact2);
    }

    /**
     * test store and read values from sqldatabase
     */
    @Test
    public void testStoreOneItemInDB() {

        ChatMessagesDataBase dataBase = new ChatMessagesDataBase(context, identity);
        ChatMessageItem[] messages;

        ChatMessageItem item = new ChatMessageItem(identity, getKeyIdentitfier(contact1), "payload", "payloadtype");
        dataBase.put(item);
        messages = dataBase.get(getKeyIdentitfier(contact1));
        assertThat(messages.length, is(1));
        compareItems(messages[0], item);

    }


    /**
     * test store and read values from sqldatabase
     */
    @Test
    public void testStoreManyItemsInDB() {

        ChatMessagesDataBase dataBase = new ChatMessagesDataBase(context, identity);
        ChatMessageItem[] messages;


        //add 30 items
        for (int i = 0; i < 30; i++) {
            ChatMessageItem item = new ChatMessageItem(identity, publicKey1, "payload" + i, "payloadtype");
            dataBase.put(item);
        }
        messages = dataBase.get(publicKey1);
        assertThat(messages.length, is(30));

    }

    /**
     * test get new message count
     */
    @Test
    public void testGetNewMessageCountFromSenderDB() {

        ChatMessagesDataBase dataBase = new ChatMessagesDataBase(context, identity);

        //add 30 items
        for (int i = 0; i < 21; i++) {
            ChatMessageItem item = new ChatMessageItem(identity, publicKey1, "payload" + i, "payloadtype");
            dataBase.put(item);
        }
        for (int i = 0; i < 6; i++) {
            ChatMessageItem item = new ChatMessageItem(identity, publicKey1, "payload2" + i, "payloadtype");
            item.sender = publicKey1;
            item.isNew = 1;
            dataBase.put(item);
        }
        int messageCount = dataBase.getNewMessageCount(contact1);
        assertThat(messageCount, is(6));
    }

    /**
     * test get new message count
     */
    @Test
    public void testSetMessagesAsRead() {

        ChatMessagesDataBase dataBase = new ChatMessagesDataBase(context, identity);
        int messageCount;
        for (int i = 0; i < 3; i++) {
            ChatMessageItem item = new ChatMessageItem(identity, publicKey1, "payload" + i, "payloadtype");
            item.sender = publicKey1;
            item.isNew = 0;
            dataBase.put(item);
        }
        for (int i = 0; i < 6; i++) {
            ChatMessageItem item = new ChatMessageItem(identity, publicKey1, "payload2" + i, "payloadtype");
            item.sender = getKeyIdentitfier(contact1);
            item.isNew = 1;
            dataBase.put(item);
        }
        messageCount = dataBase.getNewMessageCount(contact1);
        assertThat(messageCount, is(6));

        //set other as readed
        dataBase.setAllMessagesRead(contact2);
        messageCount = dataBase.getNewMessageCount(contact1);
        assertThat(messageCount, is(6));

        //set contact1 as readed
        dataBase.setAllMessagesRead(contact1);
        messageCount = dataBase.getNewMessageCount(contact1);
        assertThat(messageCount, is(0));

    }

    private String getKeyIdentitfier(Contact contact) {
        return contact.getEcPublicKey().getReadableKeyIdentifier();
    }

    /**
     * test store and read values from sqldatabase
     */
    @Test
    public void testStoreConflictItemsInDB() {

        ChatMessagesDataBase dataBase = new ChatMessagesDataBase(context, identity);
        ChatMessageItem[] messages;
        String publicKey = getKeyIdentitfier(contact1);
        //create own item1
        ChatMessageItem item = new ChatMessageItem(identity, publicKey, "payload", "payloadtype");
        ChatMessageItem item2 = new ChatMessageItem(identity, publicKey, "payload1", "payloadtype1");
        dataBase.put(item);
        messages = dataBase.get(publicKey);
        assertThat(messages.length, is(1));
        compareItems(messages[0], item);

        //put same item1 (except one item after add)
        dataBase.put(item);
        messages = dataBase.get(publicKey);
        assertThat(messages.length, is(1));
        compareItems(messages[0], item);

        //put item 2
        dataBase.put(item2);
        messages = dataBase.get(publicKey);
        assertThat(messages.length, is(2));

        //put same item1
        dataBase.put(item);
        messages = dataBase.get(publicKey);
        assertThat(messages.length, is(2));
    }

    /**
     * test store and read values from sqldatabase
     */
    @Test
    public void testStoreInDBWithDifferentContacts() {
        ChatMessagesDataBase dataBase = new ChatMessagesDataBase(context, identity);
        ChatMessageItem[] messages;

        ChatMessageItem item1 = new ChatMessageItem(identity, publicKey1, "payload", "payloadtype");
        ChatMessageItem item2 = new ChatMessageItem(identity, publicKey2, "payload", "payloadtype");
        dataBase.put(item1);
        messages = dataBase.get(publicKey2);
        assertThat(messages.length, is(0));

        //put to other contact
        dataBase.put(item2);
        messages = dataBase.get(publicKey2);
        assertThat(messages.length, is(1));
        messages = dataBase.get(publicKey1);
        assertThat(messages.length, is(1));

        messages = dataBase.getAll();
        assertThat(messages.length, is(2));


    }

    /**
     * test store and read values via chatserver
     */
    @Test
    public void testStoreInChatServer() {
        ChatServer chatServer = new ChatServer(null, context, identity);
        ChatMessageItem[] messages;
        ChatMessageItem item = new ChatMessageItem(identity, publicKey1, "payload", "payloadtype");
        chatServer.storeIntoDB(item);
        messages = chatServer.getAllMessages(contact1);
        assertThat(messages.length, is(1));

        //store again same value
        chatServer.storeIntoDB(item);
        messages = chatServer.getAllMessages(contact1);
        assertThat(messages.length, is(1));

        //store new item
        ChatMessageItem item2 = new ChatMessageItem(identity, publicKey1, "payload", "payloadtype");
        chatServer.storeIntoDB(item2);
        messages = chatServer.getAllMessages(contact1);
        assertThat(messages.length, is(2));
    }


    private void compareItems(ChatMessageItem item1, ChatMessageItem item2) {
        assertThat(item1.getData(), is(item2.getData()));
        assertThat(item1.getTime(), is(item2.getTime()));
        assertThat(item1.getSenderKey(), is(item2.getSenderKey()));
        assertThat(item1.getReceiverKey(), is(item2.getReceiverKey()));
    }

}
