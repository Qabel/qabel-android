package de.qabel.qabelbox.chat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.util.IdentityHelper;

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
                RuntimeEnvironment.application.getApplicationContext());
        now = new Date();
    }

    @Test
    public void testUpdateNotifications() throws Exception {
        List<ChatMessageInfo> messages = doUpdateNotifications();
        assertThat(shadowOf(notificationManager).size(), equalTo(1));

        List<Notification> notifications = shadowOf(notificationManager).getAllNotifications();
        Notification notification = notifications.get(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            assertThat(notification.category, equalTo(Notification.CATEGORY_MESSAGE));
        }
        assertThat(notification.when, equalTo(messages.get(0).getSent().getTime()));
        assertThat(notification.contentIntent, notNullValue());
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
}
