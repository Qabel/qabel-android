package de.qabel.qabelbox.adapter;

import android.app.Application;
import android.content.SyncResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.drop.DropMessage;
import de.qabel.qabelbox.BuildConfig;
import de.qabel.qabelbox.SimpleApplication;
import de.qabel.qabelbox.chat.ChatMessagesDataBase;
import de.qabel.qabelbox.chat.ChatServer;
import de.qabel.qabelbox.exceptions.QblStorageEntityExistsException;
import de.qabel.qabelbox.services.MockedDropConnector;
import de.qabel.qabelbox.services.RoboLocalQabelService;
import de.qabel.qabelbox.util.IdentityHelper;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;


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
    private MockedDropConnector dropConnector;

    @Before
    public void setUp() throws QblStorageEntityExistsException {
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
        dropConnector = new MockedDropConnector();
        syncAdapter = new QabelSyncAdapter(context, true);
        syncAdapter.setDropConnector(dropConnector);
        syncAdapter = spy(syncAdapter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOnPerformSync() throws Exception {
        assertThat(db1.getAll().length, is(0));
        DropMessage message = ChatServer.createTextDropMessage(identity, "foobar");
        dropConnector.sendDropMessage(message, contact2, identity, null);
        SyncResult syncResult = new SyncResult();
        syncAdapter.onPerformSync(null, null, null, null, syncResult);
        assertThat(db1.getAll().length, is(1));
        verify(syncAdapter).notifyForNewMessages(anyList());
    }
}
