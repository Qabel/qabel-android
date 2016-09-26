package de.qabel.qabelbox.storage

import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.config.AppPreference
import de.qabel.qabelbox.storage.server.AndroidBlockServer
import de.qabel.qabelbox.storage.server.BlockServer
import de.qabel.qabelbox.test.TestConstants
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class BlockServerTest() {

    lateinit var server: BlockServer

    @Before
    fun setUp() {
        server = AndroidBlockServer(AppPreference(RuntimeEnvironment.application), RuntimeEnvironment.application)
    }

    @Test
    fun testUrlForFile(){
        val uuid = "65eca15b-0ccc-46c7-9c91-a60917ff183d"
        val prefix = TestConstants.PREFIX

        val urlA = "https://block.qabel.org/api/v0/files/$prefix/$uuid"
        var url = server.urlForFile(TestConstants.PREFIX, urlA)
        assertEquals(urlA, url)

        url = server.urlForFile(TestConstants.PREFIX, "blocks/$uuid")
        assertEquals(url, "${TestConstants.BLOCK_URL}/api/v0/files/$prefix/blocks/$uuid")
    }


}
