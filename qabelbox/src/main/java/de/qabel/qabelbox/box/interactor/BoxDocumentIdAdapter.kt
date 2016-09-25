package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.chat.repository.ChatShareRepository
import de.qabel.chat.repository.entities.ShareStatus
import de.qabel.chat.service.SharingService
import de.qabel.qabelbox.box.backends.BoxHttpStorageBackend
import de.qabel.qabelbox.box.dto.*
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.provider.ShareId
import de.qabel.qabelbox.storage.server.BlockServer
import rx.Observable
import rx.lang.kotlin.single
import rx.lang.kotlin.toSingletonObservable
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject

class BoxDocumentIdAdapter @Inject constructor(private val volumeManager: VolumeManager,
                                               private val shareRepo: ChatShareRepository,
                                               private val blockServer: BlockServer,
                                               private val sharingService: SharingService) :
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
                                           documentId: DocumentId): List<ProviderEntry> =
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


    override fun download(shareId: ShareId, target: File) = single<Unit> { single ->
        val share = shareRepo.findById(shareId.boxShareId)
        if (!listOf(ShareStatus.ACCEPTED, ShareStatus.CREATED).contains(share.status)) {
            throw QblStorageException("Invalid ShareStatus for download")
        }
        sharingService.downloadShare(share, target, BoxHttpStorageBackend(blockServer, share.prefix!!))
        single.onSuccess(Unit)
    }

    override fun refreshShare(shareId: ShareId) = single<Unit> { single ->
        val share = shareRepo.findById(shareId.boxShareId)
        if (!listOf(ShareStatus.ACCEPTED, ShareStatus.CREATED, ShareStatus.UNREACHABLE).contains(share.status)) {
            single.onSuccess(Unit)
        }
        sharingService.updateShare(share, BoxHttpStorageBackend(blockServer, share.prefix!!))
        single.onSuccess(Unit)
    }
}
