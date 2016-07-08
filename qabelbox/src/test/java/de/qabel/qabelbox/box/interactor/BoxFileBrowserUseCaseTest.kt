package de.qabel.qabelbox.box.interactor

import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.util.IdentityHelper
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class BoxFileBrowserUseCaseTest {
    val identityA = IdentityHelper.createIdentity("identity", null)


    @Test
    fun initVolume() {
        val useCase = BoxFileBrowserUseCase()
        useCase.initVolume()
    }

}
