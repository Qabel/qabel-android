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

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.SimpleApplication;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class SyncAdapterChatNotificationManagerTest {

    private SyncAdapterChatNotificationManager manager;
    private Date now;
    private ChatNotificationPresenter presenter;
    private Context context;

    @Before
    public void setUp() throws Exception {
        presenter = mock(ChatNotificationPresenter.class);
        manager = new SyncAdapterChatNotificationManager(presenter, RuntimeEnvironment.application);
        now = new Date();
        context = RuntimeEnvironment.application;
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
        return new ChatMessageInfo("contact", "identity", "message",
                sent, ChatMessageInfo.MessageType.MESSAGE);
    }

    @Test
    public void testMultipleMessagesFromSingleSender() throws Throwable {
        List<ChatMessageInfo> messages = new ArrayList<>();
        ChatMessageInfo message = createExampleMessage(now);
        messages.add(message);
        messages.add(createExampleMessage(DateUtils.addMinutes(now, 1)));

        List<ChatNotification> chatNotifications = manager.constructNotifications(messages);
        assertThat(chatNotifications, hasSize(1));
        ChatNotification expected = new ChatNotification(message.getIdentityKeyId(),
                message.getContactName(), context.getString(R.string.new_messages, 2), now);
        assertThat(chatNotifications.get(0), equalTo(expected));
    }

}
