package de.qabel.qabelbox.box.interactor

import de.qabel.qabelbox.box.dto.*
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.provider.ShareId
import rx.Observable
import rx.Single
import java.io.File

interface DocumentIdAdapter {
    fun availableRoots(): List<VolumeRoot>
    fun queryChildDocuments(documentId: DocumentId): Observable<List<ProviderEntry>>
    fun download(documentId: DocumentId): Observable<ProviderDownload>
    fun upload(providerUpload: ProviderUpload): Observable<Unit>
    fun query(documentId: DocumentId): Observable<BrowserEntry>
    fun download(shareId : ShareId, target : File) : Single<Unit>
}

