package de.qabel.qabelbox.chat.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.NotificationCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Date;

import dagger.internal.Factory;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.util.IdentityHelper;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class AndroidChatNotificationPresenterTest {

    NotificationManager notificationManager;
    private ChatNotification notification;
    private AndroidChatNotificationPresenter presenter;
    private NotificationCompat.Builder builder;

    @Before
    public void setUp() throws Exception {
        notificationManager = (NotificationManager) RuntimeEnvironment.application
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notification = new ChatNotification(IdentityHelper.createIdentity("identity", null),
                "header, header", "message",
                new Date());
        presenter = new AndroidChatNotificationPresenter(RuntimeEnvironment.application,
                new Factory<NotificationCompat.Builder>() {
                    @Override
                    public NotificationCompat.Builder get() {
                        builder = spy(new NotificationCompat.Builder(RuntimeEnvironment.application));
                        return builder;
                    }
                });
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
        assertEquals(shadowOf(note).getContentText(), notification.message);
        verify(builder).setDefaults(Notification.DEFAULT_ALL);
    }

    @Test
    public void testTaggedNotification() throws Throwable {
        presenter.showNotification(notification);
        Notification notification = shadowOf(notificationManager).getAllNotifications().get(0);
        Notification actual = shadowOf(notificationManager)
                .getNotification("ChatNotification", 0);
        assertThat(actual, equalTo(notification));
    }

    @Test
    public void testNotificationShowsList() throws Throwable {
        ChatNotification notification = new ChatNotification(IdentityHelper.createIdentity("identity", null),
                "foo, bar", "message", this.notification.when);
        Intent intent = presenter.getIntent(notification);
        assertThat(intent.getStringExtra(MainActivity.ACTIVE_IDENTITY),
                equalTo(notification.identity.getKeyIdentifier()));
        assertThat(intent.getBooleanExtra(MainActivity.START_CONTACTS_FRAGMENT, false), is(true));
        assertThat(intent.getStringExtra(MainActivity.ACTIVE_CONTACT), nullValue());
    }

    @Test
    public void testNotificationShowsChat() throws Throwable {
        ChatNotification notification = new ChatNotification(
                IdentityHelper.createIdentity("identity", null),
                IdentityHelper.createContact("contact"),
                "message", this.notification.when);
        Intent intent = presenter.getIntent(notification);
        assertThat(intent.getStringExtra(MainActivity.ACTIVE_IDENTITY),
                equalTo(notification.identity.getKeyIdentifier()));
        assertThat(intent.getBooleanExtra(MainActivity.START_CONTACTS_FRAGMENT, false), is(true));
        assertThat(intent.getStringExtra(MainActivity.ACTIVE_CONTACT),
                equalTo(notification.contact.getKeyIdentifier()));
    }
}
