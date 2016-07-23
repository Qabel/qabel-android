package de.qabel.qabelbox.contacts

import de.qabel.core.config.Identity
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.core.extensions.contains
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class ExtensionTest {

    @Test
    fun testIdentityListContains(){
        val identity = IdentityHelper.createIdentity("test2", "test");
        val data = listOf<Identity>(IdentityHelper.createIdentity("test1", "test"), identity);
        assert(data.contains(identity.keyIdentifier))
    }
}
