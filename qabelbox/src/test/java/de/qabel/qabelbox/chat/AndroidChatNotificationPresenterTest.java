package de.qabel.qabelbox.chat;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

public class AndroidChatNotificationPresenterTest {

    NotificationManager notificationManager;

    @Before
    public void setUp() throws Exception {
        notificationManager = (NotificationManager) RuntimeEnvironment.application
                .getSystemService(Context.NOTIFICATION_SERVICE);
        shadowOf(notificationManager).cancelAll();
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
