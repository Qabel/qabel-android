package de.qabel.qabelbox.persistence

import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class RepositoryFactoryTest {

    @Test
    fun testSetUpAndCreate(){
        val factory = RepositoryFactory(RuntimeEnvironment.application)
        assertThat(factory.getChatDropMessageRepository(), notNullValue())
        assertThat(factory.getContactRepository(), notNullValue())
        assertThat(factory.getDropStateRepository(), notNullValue())
        assertThat(factory.getDropUrlRepository(), notNullValue())
        assertThat(factory.getIdentityRepository(), notNullValue())
        assertThat(factory.getSqlitePrefixRepository(), notNullValue())
    }

}
