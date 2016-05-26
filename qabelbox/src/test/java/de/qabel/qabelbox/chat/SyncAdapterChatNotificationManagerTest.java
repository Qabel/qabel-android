package de.qabel.qabelbox.chat;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

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
import de.qabel.qabelbox.SimpleApplication;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class SyncAdapterChatNotificationManagerTest {

    NotificationManager notificationManager;
    private SyncAdapterChatNotificationManager manager;
    private Date now;

    @Before
    public void setUp() throws Exception {
        notificationManager = (NotificationManager) RuntimeEnvironment.application
                .getSystemService(Context.NOTIFICATION_SERVICE);
        shadowOf(notificationManager).cancelAll();

        manager = new SyncAdapterChatNotificationManager(
                new AndroidChatNotificationPresenter(RuntimeEnvironment.application));
        now = new Date();
    }

    @Test
    public void testUpdateNotifications() throws Exception {
        doUpdateNotifications();
        assertThat(shadowOf(notificationManager).size(), equalTo(1));
    }

    @NonNull
    public List<ChatMessageInfo> doUpdateNotifications() {
        List<ChatMessageInfo> messages = new ArrayList<>();
        messages.add(new ChatMessageInfo("contact", "identity", "message",
                now, ChatMessageInfo.MessageType.MESSAGE));
        manager.updateNotifications(messages);
        return messages;
    }

    @Test
    public void testDuplicateNotificationsPrevented() {
        doUpdateNotifications();
        doUpdateNotifications();
        assertThat(shadowOf(notificationManager).size(), equalTo(1));
    }

    @Test
    public void testShowNotification() throws Throwable {
        ChatNotification notification = new ChatNotification("myid", "header, header", "message",
                new Date());
        new AndroidChatNotificationPresenter(RuntimeEnvironment.application).showNotification(notification);
        Notification note = shadowOf(notificationManager).getAllNotifications().get(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            assertThat(note.category, equalTo(Notification.CATEGORY_MESSAGE));
        }
        assertThat(note.when, equalTo(notification.when.getTime()));
        assertThat(note.contentIntent, notNullValue());
    }
}
