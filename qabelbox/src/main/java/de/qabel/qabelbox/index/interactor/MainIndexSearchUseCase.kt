package de.qabel.qabelbox.index.interactor

import de.qabel.core.index.IndexService
import de.qabel.core.logging.QabelLog
import de.qabel.qabelbox.contacts.dto.ContactDto
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.lang.kotlin.single
import rx.lang.kotlin.subscriber
import rx.schedulers.Schedulers
import javax.inject.Inject

class MainIndexSearchUseCase @Inject constructor(val service: IndexService) : IndexSearchUseCase, QabelLog {
    override fun search(email: String, phone: String): Observable<List<ContactDto>> =
            single<List<ContactDto>> { subscriber ->
                info("Searching for email=$email and phone=$phone")
                try {
                    service.searchContacts(email, phone)
                            .map { ContactDto(it, emptyList(), true) }
                            .let { subscriber.onSuccess(it) }
                } catch (e: Throwable) {
                    subscriber.onError(e)
                }
            }.subscribeOn(Schedulers.io()).toObservable()
}

