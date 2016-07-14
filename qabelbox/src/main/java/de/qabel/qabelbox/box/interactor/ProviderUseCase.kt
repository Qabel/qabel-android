package de.qabel.qabelbox.box.interactor

import de.qabel.qabelbox.box.dto.ProviderDownload
import de.qabel.qabelbox.box.dto.ProviderEntry
import de.qabel.qabelbox.box.dto.ProviderUpload
import de.qabel.qabelbox.box.dto.VolumeRoot
import de.qabel.qabelbox.box.provider.DocumentId
import rx.Observable

interface ProviderUseCase {
    fun avaliableRoots(): Observable<List<VolumeRoot>>
    fun queryChildDocuments(documentId: DocumentId): Observable<List<ProviderEntry>>
    fun download(documentId: DocumentId): Observable<ProviderDownload>
    fun upload(providerUpload: ProviderUpload): Observable<Unit>
    fun delete(documentId: DocumentId): Observable<Unit>
}

