package de.qabel.qabelbox.box.interactor

import android.content.Context
import de.qabel.box.storage.dto.BoxPath
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.chat.repository.ChatShareRepository
import de.qabel.chat.repository.entities.ShareStatus
import de.qabel.chat.service.SharingService
import de.qabel.client.box.documentId.DocumentId
import de.qabel.client.box.interactor.BrowserEntry
import de.qabel.client.box.interactor.FileOperationState
import de.qabel.client.box.interactor.VolumeManager
import de.qabel.client.box.interactor.VolumeRoot
import de.qabel.client.box.storage.LocalStorage
import de.qabel.core.extensions.letApply
import de.qabel.qabelbox.box.backends.BoxHttpStorageBackend
import de.qabel.qabelbox.box.dto.ProviderEntry
import de.qabel.qabelbox.box.provider.ShareId
import de.qabel.qabelbox.storage.server.BlockServer
import rx.Observable
import rx.lang.kotlin.single
import rx.lang.kotlin.toSingletonObservable
import java.io.File
import java.util.*
import javax.inject.Inject

class BoxDocumentIdAdapter @Inject constructor(context: Context,
                                               volumeManager: VolumeManager,
                                               private val shareRepo: ChatShareRepository,
                                               private val blockServer: BlockServer,
                                               private val sharingService: SharingService,
                                               private val localStorage: LocalStorage
) : BoxDocumentIdInteractor(context, volumeManager), DocumentIdAdapter {

    override fun query(documentId: DocumentId): Observable<BrowserEntry> {
        return browserByDocumentId(documentId).query(documentId.path)
    }

    override fun downloadFile(documentId: DocumentId, targetFile: File): Pair<FileOperationState, Observable<FileOperationState>> =
            downloadFile(documentId, targetFile.outputStream())


    override fun availableRoots(): List<VolumeRoot> = volumeManager.roots

    override fun queryChildDocuments(documentId: DocumentId): Observable<List<ProviderEntry>> {
        when (documentId.path) {
            is BoxPath.File -> return emptyList<ProviderEntry>().toSingletonObservable()
            is BoxPath.FolderLike -> {
                val path = documentId.path as BoxPath.FolderLike
                val listing: Observable<List<BrowserEntry>> =
                        browserByDocumentId(documentId).list(path)
                return listing.map { entries ->
                    transformToProviderEntries(entries, path, documentId)
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

    override fun download(shareId: ShareId) = single<File> { single ->
        val share = shareRepo.findById(shareId.boxShareId)
        if (!listOf(ShareStatus.ACCEPTED, ShareStatus.CREATED).contains(share.status)) {
            throw QblStorageException("Invalid ShareStatus for download")
        }
        val storageBackend = BoxHttpStorageBackend(blockServer, share.prefix!!)
        val externalBoxFile = sharingService.getBoxExternalFile(share, storageBackend)
        val sharePath = BoxPath.Root * (externalBoxFile.prefix + externalBoxFile.name)
        val resultFile = localStorage.getBoxFile(sharePath, externalBoxFile) ?: createTempFile().letApply {
            sharingService.downloadShare(share, it, storageBackend)
            localStorage.storeFile(it.inputStream(), externalBoxFile, sharePath)
        }
        single.onSuccess(resultFile)
    }

    override fun listShare(shareId : ShareId) = single<BrowserEntry.File> {
        val entry = shareRepo.findById(shareId.boxShareId).let {
            BrowserEntry.File(it.name, it.size, Date(it.modifiedOn))
        }
        it.onSuccess(entry)
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
