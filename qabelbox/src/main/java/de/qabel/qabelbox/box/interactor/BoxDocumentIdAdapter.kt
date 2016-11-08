package de.qabel.qabelbox.box.interactor

import android.content.Context
import de.qabel.box.storage.dto.BoxPath
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.chat.repository.ChatShareRepository
import de.qabel.chat.repository.entities.ShareStatus
import de.qabel.chat.service.SharingService
import de.qabel.qabelbox.box.backends.BoxHttpStorageBackend
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.FileOperationState
import de.qabel.qabelbox.box.dto.ProviderEntry
import de.qabel.qabelbox.box.dto.VolumeRoot
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.provider.ShareId
import de.qabel.qabelbox.storage.server.BlockServer
import rx.Observable
import rx.lang.kotlin.single
import rx.lang.kotlin.toSingletonObservable
import java.io.File
import javax.inject.Inject

class BoxDocumentIdAdapter @Inject constructor(context: Context,
                                               volumeManager: VolumeManager,
                                               private val shareRepo: ChatShareRepository,
                                               private val blockServer: BlockServer,
                                               private val sharingService: SharingService
) : BoxDocumentIdInteractor(context, volumeManager), DocumentIdAdapter {

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

    override fun uploadFile(sourceFile: File, targetDocumentId: DocumentId): Pair<FileOperationState, Observable<FileOperationState>> {
        sourceFile.inputStream().use {
            return uploadFile(it, targetDocumentId)
        }
    }

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
