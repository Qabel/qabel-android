package de.qabel.qabelbox.box.views

import de.qabel.qabelbox.box.provider.DocumentId
import rx.subjects.Subject

interface FileUploadView {

    fun choose(availableIdentities: List<IdentitySelection>)
    val identity: Subject<IdentitySelection, IdentitySelection>

    fun chosePath(identity: IdentitySelection)
    val path: Subject<DocumentId, DocumentId>

    val filename: Subject<String, String>

    class IdentitySelection(val alias: String, val keyId: String)
}

