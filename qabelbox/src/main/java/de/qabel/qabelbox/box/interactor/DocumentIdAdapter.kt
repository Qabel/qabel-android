package de.qabel.qabelbox.box.interactor

import de.qabel.client.box.documentId.DocumentId
import de.qabel.client.box.interactor.BrowserEntry
import de.qabel.client.box.interactor.FileOperationState
import de.qabel.client.box.interactor.VolumeRoot
import de.qabel.qabelbox.box.dto.ProviderEntry
import de.qabel.qabelbox.box.provider.ShareId
import rx.Observable
import rx.Single
import java.io.File

interface DocumentIdAdapter : DocumentIdInteractor {
    fun availableRoots(): List<VolumeRoot>
    fun queryChildDocuments(documentId: DocumentId): Observable<List<ProviderEntry>>

    fun query(documentId: DocumentId): Observable<BrowserEntry>
    fun download(shareId : ShareId) : Single<File>
    fun refreshShare(shareId: ShareId) : Single<Unit>

    fun downloadFile(documentId: DocumentId, targetFile : File): Pair<FileOperationState, Observable<FileOperationState>>
    fun uploadFile(sourceFile: File, targetDocumentId: DocumentId): Pair<FileOperationState, Observable<FileOperationState>>
}

