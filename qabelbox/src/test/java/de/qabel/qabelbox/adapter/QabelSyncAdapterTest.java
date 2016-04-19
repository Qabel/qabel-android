package de.qabel.qabelbox.adapter;

import android.app.Application;
import android.content.Context;
import android.content.SyncResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.chat.ChatMessagesDataBase;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.services.LocalQabelService;
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
    private Contact contact1;
    private Contact contact2;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        service = new RoboLocalQabelService();
        service.onCreate();
        identity = IdentityHelper.createIdentity("foo", null);
        identity2 = IdentityHelper.createIdentity("bar", null);
        service.addIdentity(identity);
        service.addIdentity(identity2);
        contact1 = new Contact("contact1", identity.getDropUrls(), identity.getEcPublicKey());
        contact2 = new Contact("contact2", identity2.getDropUrls(), identity2.getEcPublicKey());
        service.addContact(contact1, identity2);
        service.addContact(contact2, identity);
        db1 = new ChatMessagesDataBase(context, identity);
        db2 = new ChatMessagesDataBase(context, identity2);
        chatServer = new ChatServer(context);
        syncAdapter = new QabelSyncAdapter(context, true) {
            @Override
            void bindToService(Context context) {
                mService = service;
                resourcesReady = true;
            }
        };
    }

    @Test
    public void testOnPerformSync() throws Exception {
        assertThat(db1.getAll().length, is(0));
        assertThat(db2.getAll().length, is(0));
        DropMessage message = chatServer.createTextDropMessage(identity, "foobar");
        final CountDownLatch lock = new CountDownLatch(1);
        service.sendDropMessage(message, contact2, identity,
			new LocalQabelService.OnSendDropMessageResult() {
                @Override
                public void onSendDropResult(Map<DropURL, Boolean> deliveryStatus) {
                    lock.countDown();
                }
            });
        lock.await();
        SyncResult syncResult = new SyncResult();
        syncAdapter.onPerformSync(null, null, null, null, syncResult);
        assertThat(db2.getAll().length, is(1));

    }
}
