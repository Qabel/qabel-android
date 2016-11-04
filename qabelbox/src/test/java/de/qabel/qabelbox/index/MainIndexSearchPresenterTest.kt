package de.qabel.qabelbox.index

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import de.qabel.core.repository.inmemory.InMemoryContactRepository
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.index.interactor.IndexSearchUseCase
import de.qabel.qabelbox.index.view.presenters.MainIndexSearchPresenter
import de.qabel.qabelbox.index.view.views.IndexSearchView
import de.qabel.qabelbox.navigation.Navigator
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import rx.Observable
import rx.lang.kotlin.toSingletonObservable
import rx.subjects.BehaviorSubject

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class MainIndexSearchPresenterTest {

    open class StubIndexSearchView: IndexSearchView {

        var updated: String = ""

        val searchSubject: BehaviorSubject<String> = BehaviorSubject.create()

        override var searchString: Observable<String> = searchSubject

        override fun loadData(data: List<ContactDto>) { }

        override fun showEmpty() { }

        override fun showError(error: Throwable) { }

    }

    val useCase: IndexSearchUseCase = Mockito.mock(IndexSearchUseCase::class.java)
    val view = StubIndexSearchView()
    val presenter = object: MainIndexSearchPresenter(view, useCase, Mockito.mock(Navigator::class.java), InMemoryContactRepository()) {
        override fun format(term: String): String = "formatted"
    }
    val email = "test@example.com"
    val phone = "+ 49 199 12345678"
    val contact = IdentityHelper.createContact("test").apply {
        email = email
    }
    val list = listOf(ContactDto(contact, emptyList(), true))

    @Test
    fun formatPhoneNumber() {
        Mockito.stub(useCase.search(Mockito.anyString(), Mockito.anyString())).toReturn(
                emptyList<ContactDto>().toSingletonObservable())
        view.searchSubject.onNext(phone)
        Mockito.verify(useCase).search(phone, "formatted")
    }

    @Test
    fun onlySendValidPhoneNumber() {
        Mockito.stub(useCase.search(Mockito.anyString(), Mockito.anyString())).toReturn(
                list.toSingletonObservable())
        view.searchSubject.onNext("asrdf")
        Mockito.verify(useCase).search("asrdf", "")
    }
}
