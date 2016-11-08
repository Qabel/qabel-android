package de.qabel.qabelbox.box.interactor

import android.net.Uri
import de.qabel.qabelbox.box.dto.FileOperationState
import de.qabel.qabelbox.box.provider.DocumentId
import rx.Observable

interface DocumentIdInteractor {

    fun uploadFile(sourceUri: Uri, targetDocumentId: DocumentId): Pair<FileOperationState, Observable<FileOperationState>>
    fun downloadFile(documentId: DocumentId, targetUri: Uri): Pair<FileOperationState, Observable<FileOperationState>>

}
