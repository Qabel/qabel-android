package de.qabel.qabelbox.index.view.presenters

import com.google.i18n.phonenumbers.NumberParseException
import de.qabel.core.config.Contact
import de.qabel.core.index.formatPhoneNumber
import de.qabel.core.logging.QabelLog
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.exception.EntityExistsException
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.index.interactor.IndexSearchUseCase
import de.qabel.qabelbox.index.view.views.IndexSearchView
import javax.inject.Inject

class MainIndexSearchPresenter @Inject constructor(
        val view: IndexSearchView,
        val useCase: IndexSearchUseCase,
        val contactRepository: ContactRepository)
: IndexSearchPresenter, QabelLog {

    override fun search() {
        view.searchString?.let {
            if (it.isNotBlank()) {
                val phone = try { formatPhoneNumber(it) } catch (e: NumberParseException) { "" }
                if (phone.isNotBlank()) {
                    view.searchString = phone
                }
                useCase.search(it, phone).subscribe({
                    info("Index search result length: ${it.size}")
                    if (it.size > 0) {
                        view.loadData(it)
                    } else {
                        view.showEmpty()
                    }
                }, { view.showError(it) })

            }
        }
    }

    override fun showDetails(contact: ContactDto) {
        try {
            contactRepository.persist(contact.contact, contact.identities)
        } catch (ignored: EntityExistsException) {
            val contactData = contactRepository.findContactWithIdentities(contact.contact.keyIdentifier)
            if (contactData.identities.size == 0) {
                contactData.contact.status = Contact.ContactStatus.UNKNOWN
                contactData.contact.isIgnored = false
            }
            contactRepository.update(contactData.contact, emptyList())
        }
        view.showDetails(contact)
    }

}
