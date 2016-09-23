package de.qabel.qabelbox.index.view.presenters

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
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
    val view: IndexSearchView = mock()
    val presenter = MainIndexSearchPresenter(view, useCase)
    val email = "test@example.com"
    val phone = "+ 49 199 12345678"
    val contact = IdentityHelper.createContact("test").apply {
        email = email
    }
    val list = listOf(ContactDto(contact, emptyList(), true))

    @Test
    fun emptyResults() {
        stub(useCase.search(email, phone)).toReturn(
                emptyList<ContactDto>().toSingletonObservable())
        presenter.search(email, phone)
        verify(view).showEmpty()
    }

    @Test
    fun showResults() {
        stub(useCase.search(email, phone)).toReturn(
                list.toSingletonObservable())
        presenter.search(email, phone)
        verify(view).loadData(list)
    }
}
