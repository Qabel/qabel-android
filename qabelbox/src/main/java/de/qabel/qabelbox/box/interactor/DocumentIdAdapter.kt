package de.qabel.qabelbox.box.interactor

import de.qabel.qabelbox.box.dto.*
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.provider.ShareId
import rx.Observable
import rx.Single
import java.io.File

interface DocumentIdAdapter : DocumentIdInteractor {
    fun availableRoots(): List<VolumeRoot>
    fun queryChildDocuments(documentId: DocumentId): Observable<List<ProviderEntry>>

    fun query(documentId: DocumentId): Observable<BrowserEntry>
    fun download(shareId : ShareId, target : File) : Single<Unit>
    fun refreshShare(shareId: ShareId) : Single<Unit>

    fun uploadFile(sourceFile: File, targetDocumentId: DocumentId): Pair<FileOperationState, Observable<FileOperationState>>
}

