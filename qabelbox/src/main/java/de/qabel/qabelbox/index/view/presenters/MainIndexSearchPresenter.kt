package de.qabel.qabelbox.index.view.presenters

import com.google.i18n.phonenumbers.NumberParseException
import de.qabel.core.index.formatPhoneNumber
import de.qabel.core.logging.QabelLog
import de.qabel.core.repository.ContactRepository
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.index.interactor.IndexSearchUseCase
import de.qabel.qabelbox.index.view.views.IndexSearchView
import de.qabel.qabelbox.navigation.Navigator
import javax.inject.Inject

open class MainIndexSearchPresenter @Inject constructor(
        val view: IndexSearchView,
        val useCase: IndexSearchUseCase,
        val navigator : Navigator,
        val contactRepository: ContactRepository)
: IndexSearchPresenter, QabelLog {

    init {
        view.searchString.subscribe({search(it)})
    }

    fun search(query: String) {
        val term = query.trim()
        if (term.isNotBlank()) {
            val phone = try { format(term) } catch (e: NumberParseException) { "" }
            useCase.search(term, phone).subscribe({
                info("Index search result length: ${it.size}")
                if (it.size > 0) {
                    view.loadData(it)
                } else {
                    view.showEmpty()
                }
            }, { view.showError(it) })

        }
    }

    override fun showDetails(contact: ContactDto) {
        if(contact.active){
            navigator.selectContactEdit(contact)
        }
    }

    open fun format(term: String) = formatPhoneNumber(term)
}
