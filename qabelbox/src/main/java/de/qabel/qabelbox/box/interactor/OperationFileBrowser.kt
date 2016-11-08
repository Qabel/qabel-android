package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.dto.BoxPath
import de.qabel.qabelbox.box.dto.FileOperationState
import de.qabel.qabelbox.box.dto.UploadSource
import rx.Observable
import java.io.File

interface OperationFileBrowser : ReadFileBrowser {

    fun upload(path: BoxPath.File, source: UploadSource): Pair<FileOperationState, Observable<FileOperationState>>
    fun download(path: BoxPath.File, targetFile: File): Pair<FileOperationState, Observable<FileOperationState>>

    fun createFolder(path: BoxPath.FolderLike): Observable<Unit>
    fun delete(path: BoxPath): Observable<Unit>

}
