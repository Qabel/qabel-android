package de.qabel.qabelbox.index.view.presenters

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.stub
import de.qabel.core.repository.inmemory.InMemoryContactRepository
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.SimpleApplication
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.index.interactor.IndexSearchUseCase
import de.qabel.qabelbox.index.view.views.IndexSearchView
import de.qabel.qabelbox.util.IdentityHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.robolectric.RobolectricGradleTestRunner
import org.robolectric.annotation.Config
import rx.lang.kotlin.toSingletonObservable

@RunWith(RobolectricGradleTestRunner::class)
@Config(application = SimpleApplication::class, constants = BuildConfig::class)
class MainIndexSearchPresenterTest {

    val useCase: IndexSearchUseCase = mock()
    val view: IndexSearchView = object: IndexSearchView {

        override var searchString: String? = null

        override fun loadData(data: List<ContactDto>) { }

        override fun showEmpty() { }

        override fun showDetails(contact: ContactDto) { }

        override fun showError(error: Throwable) { }

    }
    val viewSpy: IndexSearchView = spy(view)
    val presenter = MainIndexSearchPresenter(viewSpy, useCase, InMemoryContactRepository())
    val email = "test@example.com"
    val phone = "+ 49 199 12345678"
    val contact = IdentityHelper.createContact("test").apply {
        email = email
    }
    val list = listOf(ContactDto(contact, emptyList(), true))

    @Test
    fun emptyResults() {
        stub(useCase.search(email, email)).toReturn(
                emptyList<ContactDto>().toSingletonObservable())
        view.searchString = email
        presenter.search()
        verify(view).showEmpty()
    }

    @Test
    fun showResults() {
        stub(useCase.search(email, email)).toReturn(
                list.toSingletonObservable())
        view.searchString = email
        presenter.search()
        verify(view).loadData(list)
    }
}
