package de.qabel.qabelbox.box.interactor

import de.qabel.qabelbox.box.dto.*
import de.qabel.qabelbox.box.provider.DocumentId
import rx.Observable
import rx.lang.kotlin.toSingletonObservable
import java.io.FileNotFoundException

class BoxProviderUseCase(private val volumeManager: VolumeManager) : ProviderUseCase {

    override fun query(documentId: DocumentId): Observable<BrowserEntry> {
        return browserByDocumentId(documentId).query(documentId.path)
    }

    override fun availableRoots(): List<VolumeRoot> = volumeManager.roots

    override fun queryChildDocuments(documentId: DocumentId): Observable<List<ProviderEntry>> {
        when (documentId.path) {
            is BoxPath.File -> return emptyList<ProviderEntry>().toSingletonObservable()
            is BoxPath.FolderLike -> {
                return browserByDocumentId(documentId).list(documentId.path).map { entries ->
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
        when (documentId.path) {
            is BoxPath.FolderLike -> return Observable.error(FileNotFoundException("Not a file"))
            is BoxPath.File -> {
                val browserUseCase = browserByDocumentId(documentId)
                return browserUseCase.download(documentId.path).map {
                    ProviderDownload(documentId, it)
                }
            }
        }
    }

    override fun upload(providerUpload: ProviderUpload): Observable<Unit> {
        TODO()
    }

    override fun delete(documentId: DocumentId): Observable<Unit> {
        TODO()
    }

    private fun browserByDocumentId(documentId: DocumentId)
            = volumeManager.fileBrowser(documentId.copy(path = BoxPath.Root).toString())


}
