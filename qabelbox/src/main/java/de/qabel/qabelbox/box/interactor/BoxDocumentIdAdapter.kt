package de.qabel.qabelbox.box.interactor

import de.qabel.qabelbox.box.dto.*
import de.qabel.qabelbox.box.provider.DocumentId
import rx.Observable
import rx.lang.kotlin.toSingletonObservable
import java.io.FileNotFoundException
import javax.inject.Inject

class BoxDocumentIdAdapter @Inject constructor(private val volumeManager: VolumeManager):
        DocumentIdAdapter {

    override fun query(documentId: DocumentId): Observable<BrowserEntry> {
        return browserByDocumentId(documentId).query(documentId.path)
    }

    override fun availableRoots(): List<VolumeRoot> = volumeManager.roots

    override fun queryChildDocuments(documentId: DocumentId): Observable<List<ProviderEntry>> {
        when (documentId.path) {
            is BoxPath.File -> return emptyList<ProviderEntry>().toSingletonObservable()
            is BoxPath.FolderLike -> {
                val listing: Observable<List<BrowserEntry>> =
                        browserByDocumentId(documentId).list(documentId.path)
                return listing.map { entries ->
                    transformToProviderEntries(entries, documentId.path, documentId)
                }
            }
        }
    }

    private fun transformToProviderEntries(entries: List<BrowserEntry>,
                                           root: BoxPath.FolderLike,
                                           documentId: DocumentId):List<ProviderEntry> =
        entries.map {
            val path = when (it) {
                is BrowserEntry.File -> root * it.name
                is BrowserEntry.Folder -> root / it.name
            }
            ProviderEntry(documentId.copy(path = path), it)
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
        val path = providerUpload.documentId.path
        when (path) {
            is BoxPath.FolderLike -> return Observable.error(FileNotFoundException("Not a file"))
            is BoxPath.File -> {
                return browserByDocumentId(providerUpload.documentId).upload(path, providerUpload.source)
            }
        }
    }

    private fun browserByDocumentId(documentId: DocumentId)
            = volumeManager.fileBrowser(documentId.copy(path = BoxPath.Root).toString())


}
