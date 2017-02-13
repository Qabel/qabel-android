package de.qabel.qabelbox.identity

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import de.qabel.core.extensions.CoreTestCase
import de.qabel.core.extensions.createIdentity
import de.qabel.core.index.formatPhoneNumber
import de.qabel.core.index.randomPhone
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.eq
import de.qabel.qabelbox.identity.interactor.IdentityInteractor
import de.qabel.qabelbox.identity.view.IdentityDetailsView
import de.qabel.qabelbox.identity.view.presenter.IdentityDetailsPresenter
import de.qabel.qabelbox.identity.view.presenter.MainIdentityDetailsPresenter
import de.qabel.qabelbox.navigation.Navigator
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import rx.lang.kotlin.singleOf
import rx.lang.kotlin.toSingletonObservable
import java.util.*

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class IdentityDetailsPresenterTest() : CoreTestCase {

    val identity = createIdentity("Bob")

    val view: IdentityDetailsView = mock<IdentityDetailsView>().apply {
        stub(identityKeyId).toReturn(identity.keyIdentifier)
    }

    val useCase: IdentityInteractor = mock<IdentityInteractor>().apply {
        stub(getIdentity(identity.keyIdentifier)).toReturn(identity)
        stub(saveIdentity(identity)).toReturn(singleOf(identity))
    }

    val navigator: Navigator = mock()

    val presenter: IdentityDetailsPresenter = MainIdentityDetailsPresenter(view, useCase, navigator)

    @Test
    fun testLoad() {
        presenter.loadIdentity()
        verify(view).loadIdentity(identity)
        presenter.identity eq identity
    }

    @Test
    fun testSaveAlias() {
        presenter.identity = identity
        presenter.onSaveAlias("Alice")
        verify(view).loadIdentity(identity)
        identity.alias eq "Alice"
    }

    @Test
    @Ignore("not running in roboelectic")
    fun testSavePhone() {
        presenter.identity = identity
        val random = randomPhone()
        val formatted = formatPhoneNumber(random)
        presenter.onSavePhoneNumber(random)
        verify(view).loadIdentity(identity)
        identity.phone eq formatted
    }

    @Test
    fun testSaveEmail() {
        presenter.identity = identity
        presenter.onSaveEmail("mail@test.de")
        verify(view).loadIdentity(identity)
        identity.email eq "mail@test.de"
    }

    @Test
    fun testOnQRClick() {
        presenter.identity = identity
        presenter.onShowQRClick()
        verify(navigator).selectQrCodeFragment(identity.toContact())
    }

}
