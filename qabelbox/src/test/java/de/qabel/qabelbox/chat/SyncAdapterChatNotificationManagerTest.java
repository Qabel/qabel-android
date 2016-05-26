package de.qabel.qabelbox.chat;

import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.SimpleApplication;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class SyncAdapterChatNotificationManagerTest {

    private SyncAdapterChatNotificationManager manager;
    private Date now;
    private ChatNotificationPresenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter = mock(ChatNotificationPresenter.class);
        manager = new SyncAdapterChatNotificationManager(presenter);
        now = new Date();
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
        messages.add(new ChatMessageInfo("contact", "identity", "message",
                now, ChatMessageInfo.MessageType.MESSAGE));
        manager.updateNotifications(messages);
        return messages;
    }

}
