package de.qabel.qabelbox.box.interactor

import de.qabel.qabelbox.box.dto.BoxPath
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.DownloadSource
import de.qabel.qabelbox.box.dto.UploadSource
import rx.Observable

interface FileBrowserUseCase {
    fun upload(path: BoxPath.File, source: UploadSource): Observable<Unit>
    fun download(path: BoxPath.File):  Observable<DownloadSource>
    fun delete(path: BoxPath):  Observable<Unit>
    fun list(path: BoxPath.Folder): Observable<BrowserEntry>
    fun createFolder(path: BoxPath.Folder): Observable<Unit>
}

