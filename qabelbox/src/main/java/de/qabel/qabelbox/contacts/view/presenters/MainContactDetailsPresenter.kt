package de.qabel.qabelbox.contacts.view.presenters

import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.interactor.ContactsUseCase
import de.qabel.qabelbox.contacts.view.views.ContactDetailsView
import org.jetbrains.anko.AnkoLogger
import javax.inject.Inject

class MainContactDetailsPresenter @Inject constructor(private val view: ContactDetailsView,
                                                     private val useCase: ContactsUseCase) : ContactDetailsPresenter, AnkoLogger {
    override val title: String
        get() = contact?.contact?.alias ?:  ""

    var contact : ContactDto? = null

    override fun refreshContact(){
        useCase.loadContact(view.contactKeyId).subscribe { contact -> this.contact = contact; view.loadContact(contact); }
    }

}
