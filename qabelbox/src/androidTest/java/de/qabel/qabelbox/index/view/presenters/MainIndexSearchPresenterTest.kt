package de.qabel.qabelbox.index.view.presenters

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import de.qabel.core.repository.inmemory.InMemoryContactRepository
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.index.interactor.IndexSearchUseCase
import de.qabel.qabelbox.index.view.views.IndexSearchView
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Test
import org.mockito.Mockito
import rx.lang.kotlin.toSingletonObservable

class MainIndexSearchPresenterTest {

    open class StubIndexSearchView: IndexSearchView {
        override var searchString: String? = null

        override fun loadData(data: List<ContactDto>) { }

        override fun showEmpty() { }

        override fun showError(error: Throwable) { }

        override fun showDetails(contactDto: ContactDto) { }

    }

    val useCase: IndexSearchUseCase = Mockito.mock(IndexSearchUseCase::class.java)
    val view: IndexSearchView = StubIndexSearchView()
    val presenter = MainIndexSearchPresenter(view, useCase, InMemoryContactRepository())
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
        view.searchString = phone
        presenter.search()
        assertThat(view.searchString, equalTo("+4919912345678" as String?))
    }

    @Test
    fun onlySendValidPhoneNumber() {
        Mockito.stub(useCase.search(Mockito.anyString(), Mockito.anyString())).toReturn(
                list.toSingletonObservable())
        view.searchString = "asrdf"
        presenter.search()
        Mockito.verify(useCase).search("asrdf", "")
    }
}
