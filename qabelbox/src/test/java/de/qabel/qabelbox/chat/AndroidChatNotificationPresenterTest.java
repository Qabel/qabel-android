package de.qabel.qabelbox.chat;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Date;

import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.SimpleApplication;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class AndroidChatNotificationPresenterTest {

    NotificationManager notificationManager;
    private ChatNotification notification;
    private AndroidChatNotificationPresenter presenter;

    @Before
    public void setUp() throws Exception {
        notificationManager = (NotificationManager) RuntimeEnvironment.application
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notification = new ChatNotification("myid", "header, header", "message",
                new Date());
        presenter = new AndroidChatNotificationPresenter(RuntimeEnvironment.application);
    }

    @Test
    public void testShowNotification() throws Throwable {
        presenter.showNotification(notification);
        Notification note = shadowOf(notificationManager).getAllNotifications().get(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            assertThat(note.category, equalTo(Notification.CATEGORY_MESSAGE));
        }
        assertThat(note.when, equalTo(notification.when.getTime()));
        assertThat(note.contentIntent, notNullValue());
    }

    @Test
    public void testTaggedNotification() throws Throwable {
        presenter.showNotification(notification);
        Notification notification = shadowOf(notificationManager).getAllNotifications().get(0);
        Notification actual = shadowOf(notificationManager)
                .getNotification("ChatNotification", 0);
        assertThat(actual, equalTo(notification));
    }


}
