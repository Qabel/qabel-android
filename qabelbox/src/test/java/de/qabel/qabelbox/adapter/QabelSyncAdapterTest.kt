package de.qabel.qabelbox.adapter

import android.app.Application
import android.content.SyncResult
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.chat.service.ChatService
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.persistence.RepositoryFactory
import de.qabel.qabelbox.sync.QabelSyncAdapter
import de.qabel.qabelbox.util.IdentityHelper
import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * XXX Add Code test and refactor
 */
@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class QabelSyncAdapterTest {

    @Test
    fun placeHolder(){
        assert(true)
    }

    /*  lateinit var context: Application
      lateinit var identity: Identity
      lateinit var identity2: Identity
      lateinit var syncAdapter: QabelSyncAdapter
      lateinit var chatService: ChatService
      lateinit var contact1: Contact
      lateinit var contact2: Contact
      lateinit var dropConnector: MockedDropConnector

      @Before
      fun setUp() {
          context = RuntimeEnvironment.application
          val factory = RepositoryFactory(context)
          identity = IdentityHelper.createIdentity("foo", null)
          identity2 = IdentityHelper.createIdentity("bar", null)
          val identityRepository = factory.getIdentityRepository(factory.androidClientDatabase)
          identityRepository.save(identity)
          identityRepository.save(identity2)
          contact1 = Contact("contact1", identity.dropUrls, identity.ecPublicKey)
          contact2 = Contact("contact2", identity2.dropUrls, identity2.ecPublicKey)
          val contactRepository = factory.getContactRepository(factory.androidClientDatabase)
          contactRepository.save(contact1, identity2)
          contactRepository.save(contact2, identity)
          chatService = ChatService(context)
          dropConnector = MockedDropConnector()
          syncAdapter = QabelSyncAdapter(context, true)
          syncAdapter.setDropConnector(dropConnector)
          syncAdapter = spy(syncAdapter)
      }
  */
    /**
     * Looks like the problem is,
     * that the senderKeyId is not set when creating object with Identity Object
     */
    /* @Test
     @Ignore("Invalid test. SenderKey is null in created dropmessage!")
     fun testOnPerformSync() {
         assertThat(db1.all.size, `is`(0))
         val message = ChatServer.createTextDropMessage(identity, "foobar")
         dropConnector.sendDropMessage(message, contact2, identity, null)
         val syncResult = SyncResult()
         syncAdapter.onPerformSync(mock(), mock(), "", mock(), syncResult)
         assertThat(db1.all.size, `is`(1))
         verify<QabelSyncAdapter>(syncAdapter).notifyForNewMessages(any())
     }*/
}
