package de.qabel.qabelbox.chat;

import android.content.Context;
import android.support.annotation.NonNull;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.util.IdentityHelper;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class SyncAdapterChatNotificationManagerTest {

    private SyncAdapterChatNotificationManager manager;
    private Date now;
    private ChatNotificationPresenter presenter;
    private Context context;
    private Contact contact;
    private Identity identity;

    @Before
    public void setUp() throws Exception {
        presenter = mock(ChatNotificationPresenter.class);
        manager = new SyncAdapterChatNotificationManager(presenter, RuntimeEnvironment.application);
        now = new Date();
        context = RuntimeEnvironment.application;
        contact = IdentityHelper.createContact("contact");
        identity = IdentityHelper.createIdentity("identity", null);
    }

    @Test
    public void testUpdateNotifications() throws Exception {
        doUpdateNotifications();
        verify(presenter).showNotification(any());
    }

    @Test
    public void testDuplicateNotificationsPrevented() {
        doUpdateNotifications();
        doUpdateNotifications();
        verify(presenter).showNotification(any());
    }


    @NonNull
    public List<ChatMessageInfo> doUpdateNotifications() {
        List<ChatMessageInfo> messages = new ArrayList<>();
        messages.add(createExampleMessage(now));
        manager.updateNotifications(messages);
        return messages;
    }

    @NonNull
    public ChatMessageInfo createExampleMessage(Date sent) {
        return new ChatMessageInfo(
                contact,
                identity, "message",
                sent, ChatMessageInfo.MessageType.MESSAGE);
    }

    @Test
    public void testMultipleMessagesFromSingleSender() throws Throwable {
        List<ChatMessageInfo> messages = new ArrayList<>();
        ChatMessageInfo message = createExampleMessage(now);
        messages.add(message);
        messages.add(createExampleMessage(DateUtils.addMinutes(now, 1)));

        ChatNotification notification = manager.constructNotification(messages);
        ChatNotification expected = new ChatNotification(message.getIdentity(),
                message.getContact(), context.getString(R.string.new_messages, 2), now);
        assertThat(notification, equalTo(expected));
    }

    @Test
    public void testOneMessageToDifferentIdentities() throws Throwable {
        List<ChatMessageInfo> messages = new ArrayList<>();
        ChatMessageInfo message = createExampleMessage(now);
        ChatMessageInfo secondMessage = createExampleMessage(now);
        messages.add(message);
        messages.add(secondMessage);
        ChatNotification expected = new ChatNotification(message.getIdentity(),
                message.getContact(), message.getMessage(), now);
        ChatNotification secondExpected = new ChatNotification(secondMessage.getIdentity(),
                secondMessage.getContact(), secondMessage.getMessage(), now);

        manager.updateNotifications(messages);
        verify(presenter).showNotification(expected);
        verify(presenter).showNotification(secondExpected);
    }

    @Test
    public void testMultipleSendersToSingleIdentity() throws Throwable {
        List<ChatMessageInfo> messages = new ArrayList<>();
        ChatMessageInfo message = createExampleMessage(now);
        ChatMessageInfo secondMessage = createExampleMessage(DateUtils.addMinutes(now, 1));
        secondMessage.setContact(IdentityHelper.createContact("second"));
        messages.add(message);
        messages.add(secondMessage);

        ChatNotification notification = manager.constructNotification(messages);
        assertThat(notification, notNullValue());
        assertThat(notification.contact, nullValue());
        assertThat(notification.message, equalTo(context.getString(R.string.new_messages, 2)));
        assertThat(notification.contactHeader, equalTo(
                message.getContact().getAlias() + ", " + secondMessage.getContact().getAlias()));
    }

    @Test
    public void testNoNotification() throws Throwable {
        assertThat(manager.constructNotification(new ArrayList<>()), nullValue());
    }
}
