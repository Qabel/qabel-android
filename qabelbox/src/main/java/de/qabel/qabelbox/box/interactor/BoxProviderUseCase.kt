package de.qabel.qabelbox.box.interactor

import de.qabel.qabelbox.box.dto.ProviderDownload
import de.qabel.qabelbox.box.dto.ProviderEntry
import de.qabel.qabelbox.box.dto.ProviderUpload
import de.qabel.qabelbox.box.dto.VolumeRoot
import de.qabel.qabelbox.box.provider.DocumentId
import rx.Observable

class BoxProviderUseCase(private val volumeManager: VolumeManager) : ProviderUseCase {

    override fun availableRoots(): List<VolumeRoot> = volumeManager.roots

    override fun queryChildDocuments(documentId: DocumentId): Observable<List<ProviderEntry>> {
        TODO()
    }

    override fun download(documentId: DocumentId): Observable<ProviderDownload> {
        TODO()
    }

    override fun upload(providerUpload: ProviderUpload): Observable<Unit> {
        TODO()
    }

    override fun delete(documentId: DocumentId): Observable<Unit> {
        TODO()
    }

}
