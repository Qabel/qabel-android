package de.qabel.qabelbox.index.interactor

import de.qabel.core.index.IndexService
import de.qabel.core.logging.QabelLog
import de.qabel.core.repository.ContactRepository
import de.qabel.qabelbox.contacts.dto.ContactDto
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.lang.kotlin.single
import rx.lang.kotlin.subscriber
import rx.schedulers.Schedulers
import javax.inject.Inject

class MainIndexSearchUseCase @Inject constructor(val service: IndexService, val contactRepo: ContactRepository) : IndexSearchUseCase, QabelLog {
    override fun search(email: String, phone: String): Observable<List<ContactDto>> =
            single<List<ContactDto>> { subscriber ->
                info("Searching for email=$email and phone=$phone")
                try {
                    service.searchContacts(email, phone)
                            .map {
                                if (contactRepo.exists(it)) {
                                    val data = contactRepo.findContactWithIdentities(it.keyIdentifier)
                                    if(!it.email.isNullOrBlank()){
                                        data.contact.email = it.email
                                    }
                                    if(!it.phone.isNullOrBlank()){
                                        data.contact.phone = it.phone
                                    }
                                    ContactDto(data.contact, data.identities, !data.isIdentity)
                                } else {
                                    ContactDto(it, emptyList(), true)
                                }
                            }
                            .let { subscriber.onSuccess(it) }
                } catch (e: Throwable) {
                    subscriber.onError(e)
                }
            }.subscribeOn(Schedulers.io()).toObservable()
}

