package de.qabel.qabelbox.identity

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import de.qabel.core.extensions.CoreTestCase
import de.qabel.core.extensions.createIdentity
import de.qabel.qabelbox.eq
import de.qabel.qabelbox.identity.interactor.IdentityUseCase
import de.qabel.qabelbox.identity.view.IdentityDetailsView
import de.qabel.qabelbox.identity.view.presenter.IdentityDetailsPresenter
import de.qabel.qabelbox.identity.view.presenter.MainIdentityDetailsPresenter
import de.qabel.qabelbox.navigation.Navigator
import org.junit.Test
import rx.lang.kotlin.singleOf
import rx.lang.kotlin.toSingletonObservable

class IdentityDetailsPresenterTest() : CoreTestCase {

    val identity = createIdentity("Bob")

    val view: IdentityDetailsView = mock<IdentityDetailsView>().apply {
        stub(identityKeyId).toReturn(identity.keyIdentifier)
    }
    val useCase: IdentityUseCase = mock<IdentityUseCase>().apply {
        stub(getIdentity(identity.keyIdentifier)).toReturn(identity.toSingletonObservable().toSingle())
        stub(updateIdentity(identity)).toReturn(singleOf(Unit))
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
    fun testSavePhone() {
        presenter.identity = identity
        presenter.onSavePhoneNumber("12345678910")
        verify(view).loadIdentity(identity)
        identity.phone eq "12345678910"
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
