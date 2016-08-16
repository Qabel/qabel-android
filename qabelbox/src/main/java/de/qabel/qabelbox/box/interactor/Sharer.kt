package de.qabel.qabelbox.box.interactor

import de.qabel.core.config.Contact
import de.qabel.qabelbox.box.dto.BoxPath
import rx.Observable

interface Sharer {
    fun sendFileShare(contact: Contact, path: BoxPath): Observable<Unit>
}
