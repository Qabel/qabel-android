package de.qabel.qabelbox.box.interactor

import de.qabel.qabelbox.box.dto.*
import de.qabel.qabelbox.box.provider.DocumentId
import rx.Observable
import rx.lang.kotlin.toSingletonObservable

class BoxProviderUseCase(private val volumeManager: VolumeManager) : ProviderUseCase {

    override fun availableRoots(): List<VolumeRoot> = volumeManager.roots

    override fun queryChildDocuments(documentId: DocumentId): Observable<List<ProviderEntry>> {
        when (documentId.path) {
            is BoxPath.File -> return emptyList<ProviderEntry>().toSingletonObservable()
            is BoxPath.FolderLike -> {
                val browser = volumeManager.fileBrowser(documentId.copy(path = BoxPath.Root).toString())
                return browser.list(documentId.path).map { entries ->
                    entries.map {
                        val path = when (it) {
                            is BrowserEntry.File -> documentId.path * it.name
                            is BrowserEntry.Folder -> documentId.path / it.name
                        }
                        ProviderEntry(documentId.copy(path = path), it)
                    }
                }
            }
        }
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
