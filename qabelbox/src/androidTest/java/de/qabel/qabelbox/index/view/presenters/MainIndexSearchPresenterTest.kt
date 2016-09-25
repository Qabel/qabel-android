package de.qabel.qabelbox.index.view.presenters

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import de.qabel.core.repository.inmemory.InMemoryContactRepository
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.index.interactor.IndexSearchUseCase
import de.qabel.qabelbox.index.view.views.IndexSearchView
import de.qabel.qabelbox.navigation.Navigator
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Test
import org.mockito.Mockito
import rx.Observable
import rx.lang.kotlin.toSingletonObservable
import rx.subjects.BehaviorSubject


class MainIndexSearchPresenterTest {

    open class StubIndexSearchView: IndexSearchView {

        var updated: String = ""

        override fun updateQuery(query: String) {
            updated = query
        }

        val searchSubject: BehaviorSubject<String> = BehaviorSubject.create()

        override var searchString: Observable<String> = searchSubject

        override fun loadData(data: List<ContactDto>) { }

        override fun showEmpty() { }

        override fun showError(error: Throwable) { }

    }

    val useCase: IndexSearchUseCase = Mockito.mock(IndexSearchUseCase::class.java)
    val view = StubIndexSearchView()
    val presenter = MainIndexSearchPresenter(view, useCase, Mockito.mock(Navigator::class.java), InMemoryContactRepository())
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
        assertThat(view.updated, equalTo("+4919912345678"))
    }

    @Test
    fun onlySendValidPhoneNumber() {
        Mockito.stub(useCase.search(Mockito.anyString(), Mockito.anyString())).toReturn(
                list.toSingletonObservable())
        view.searchSubject.onNext("asrdf")
        Mockito.verify(useCase).search("asrdf", "")
    }
}
