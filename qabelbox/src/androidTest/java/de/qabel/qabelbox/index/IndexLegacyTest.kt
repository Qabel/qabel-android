package de.qabel.qabelbox.index

import android.net.ConnectivityManager
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import de.qabel.core.index.IndexServiceTest
import de.qabel.core.index.server.IndexHTTP
import de.qabel.core.index.server.IndexHTTPLocation
import de.qabel.core.logging.QabelLog
import org.apache.http.impl.client.HttpClients
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IndexLegacyTest : IndexServiceTest(), QabelLog {

    override val testServerLocation: IndexHTTPLocation by lazy { IndexHTTPLocation("http://192.168.1.11:9698") }

    @Before
    fun checkConnection() {
        indexServer = IndexHTTP(testServerLocation, HttpClients.createDefault())
        val manager = InstrumentationRegistry.getTargetContext().getSystemService(ConnectivityManager::class.java)
        manager.activeNetworkInfo?.apply {
            assert(isAvailable)
            assert(isConnectedOrConnecting)
            debug()
        } ?: throw RuntimeException("No network available!")
    }

}
