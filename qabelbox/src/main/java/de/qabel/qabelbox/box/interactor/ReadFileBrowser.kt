package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.dto.BoxPath
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.DownloadSource
import de.qabel.qabelbox.box.dto.FileOperationState
import de.qabel.qabelbox.box.dto.UploadSource
import de.qabel.qabelbox.box.provider.DocumentId
import rx.Observable
import java.io.File

interface ReadFileBrowser {
    fun list(path: BoxPath.FolderLike): Observable<List<BrowserEntry>>
    fun query(path: BoxPath): Observable<BrowserEntry>
    fun asDocumentId(path: BoxPath): Observable<DocumentId>
}

