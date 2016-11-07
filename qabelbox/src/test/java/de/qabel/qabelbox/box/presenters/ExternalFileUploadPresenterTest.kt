package de.qabel.qabelbox.box.presenters

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import de.qabel.box.storage.dto.BoxPath
import de.qabel.core.config.Identities
import de.qabel.core.config.Identity
import de.qabel.core.config.factory.DropUrlGenerator
import de.qabel.core.config.factory.IdentityBuilder
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.box.views.FileUploadView
import de.qabel.qabelbox.eq
import de.qabel.qabelbox.identity.interactor.ReadOnlyIdentityInteractor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import rx.Single
import rx.lang.kotlin.toSingletonObservable

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class ExternalFileUploadPresenterTest {

    val dropGen = DropUrlGenerator("http://example.com")
    val identities = Identities().apply {
        put(IdentityBuilder(dropGen).withAlias("first").build())
        put(IdentityBuilder(dropGen).withAlias("second").build())
    }
    val identityInteractor: ReadOnlyIdentityInteractor = object: ReadOnlyIdentityInteractor {
        override fun getIdentity(keyId: String): Single<Identity> =
            identities.getByKeyIdentifier(keyId).toSingletonObservable().toSingle()

        override fun getIdentities(): Single<Identities> = identities.toSingletonObservable().toSingle()
    }

    val view : FileUploadView = mock()
    val presenter = ExternalFileUploadPresenter(view, identityInteractor)

    @Test
    fun availableIdentities() {
        presenter.availableIdentities.map { it.alias } eq identities.identities.map { it.alias }
    }

    @Test
    fun defaultPath() {
        presenter.defaultPath eq BoxPath.Root / "public"
    }

    @Test
    fun confirm() {
        presenter.confirm()
    }

}
