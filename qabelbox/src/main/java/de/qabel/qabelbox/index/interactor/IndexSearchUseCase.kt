package de.qabel.qabelbox.index.interactor

import de.qabel.qabelbox.contacts.dto.ContactDto
import rx.Observable
import rx.Single

interface IndexSearchUseCase {
    fun search(email: String, phone: String): Observable<List<ContactDto>>
}

