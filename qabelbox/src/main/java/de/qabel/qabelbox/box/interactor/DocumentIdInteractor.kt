package de.qabel.qabelbox.box.interactor

import android.net.Uri
import de.qabel.box.storage.dto.BoxPath
import de.qabel.qabelbox.box.dto.FileOperationState
import de.qabel.qabelbox.box.provider.DocumentId
import rx.Observable
import java.io.File

interface DocumentIdInteractor {

    fun uploadFile(sourceUri: Uri, targetDocumentId: DocumentId): Pair<FileOperationState, Observable<FileOperationState>>
    fun downloadFile(documentId: DocumentId, targetFile: File): Pair<FileOperationState, Observable<FileOperationState>>
}
