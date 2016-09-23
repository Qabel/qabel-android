package de.qabel.qabelbox.index.interactor

import de.qabel.core.index.IndexService
import de.qabel.qabelbox.contacts.dto.ContactDto
import rx.Observable
import rx.lang.kotlin.single

class MainIndexSearchUseCase(val service: IndexService) : IndexSearchUseCase {
    override fun search(email: String, phone: String): Observable<List<ContactDto>> =
            single<List<ContactDto>> {
                service.searchContacts(email, phone).map { ContactDto(it, emptyList(), true) }
            }.toObservable()
}

