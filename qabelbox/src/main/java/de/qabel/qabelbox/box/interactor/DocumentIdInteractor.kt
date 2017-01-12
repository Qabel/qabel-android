package de.qabel.qabelbox.box.interactor

import android.net.Uri
import de.qabel.client.box.documentId.DocumentId
import de.qabel.client.box.interactor.FileOperationState
import rx.Observable

interface DocumentIdInteractor {

    fun uploadFile(sourceUri: Uri, targetDocumentId: DocumentId): Pair<FileOperationState, Observable<FileOperationState>>
    fun downloadFile(documentId: DocumentId, targetUri: Uri): Pair<FileOperationState, Observable<FileOperationState>>

    fun deletePath(documentId: DocumentId): Observable<Unit>
    fun createFolder(documentId: DocumentId): Observable<Unit>
}
