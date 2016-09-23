package de.qabel.qabelbox.index.view.presenters

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import de.qabel.core.repository.inmemory.InMemoryContactRepository
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.eq
import de.qabel.qabelbox.index.interactor.IndexSearchUseCase
import de.qabel.qabelbox.index.view.views.IndexSearchView
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Test
import rx.lang.kotlin.toSingletonObservable

class MainIndexSearchPresenterTest {

    open class StubIndexSearchView: IndexSearchView {
        override var searchString: String? = null

        override fun loadData(data: List<ContactDto>) { }

        override fun showEmpty() { }

        override fun showError(error: Throwable) { }

        override fun showDetails(contactDto: ContactDto) { }

    }

    val useCase: IndexSearchUseCase = mock()
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
        stub(useCase.search(any(), any())).toReturn(
                emptyList<ContactDto>().toSingletonObservable())
        view.searchString = phone + " "
        presenter.search()
        verify(useCase).search(phone, phone)
        view.searchString eq phone
    }

    @Test
    fun onlySendValidPhoneNumber() {
        stub(useCase.search(any(), "")).toReturn(
                list.toSingletonObservable())
        view.searchString = "12345"
        presenter.search()
        verify(useCase).search("12345", "")
    }
}
