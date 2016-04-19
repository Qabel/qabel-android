package de.qabel.qabelbox.adapter;

import android.app.Application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.chat.ChatMessagesDataBase;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.services.RoboLocalQabelService;
import de.qabel.qabelbox.util.IdentityHelper;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;


@RunWith(RobolectricGradleTestRunner.class)
@Config(application = SimpleApplication.class, constants = BuildConfig.class)
public class QabelSyncAdapterTest {
    private Application context;
    private RoboLocalQabelService service;
    private Identity identity;
    private Identity identity2;
    private QabelSyncAdapter syncAdapter;
    private ChatMessagesDataBase db1;
    private ChatMessagesDataBase db2;
    private ChatServer chatServer;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        service = new RoboLocalQabelService();
        service.onCreate();
        identity = IdentityHelper.createIdentity("foo", null);
        identity2 = IdentityHelper.createIdentity("bar", null);
        service.addIdentity(identity);
        service.addIdentity(identity2);
        syncAdapter = new QabelSyncAdapter(context, true);
        db1 = new ChatMessagesDataBase(context, identity);
        db2 = new ChatMessagesDataBase(context, identity2);
        chatServer = new ChatServer(context);
    }

    @Test
    public void testOnPerformSync() throws Exception {
        assertThat(db1.getAll().length, is(0));
        assertThat(db2.getAll().length, is(0));

    }
}
