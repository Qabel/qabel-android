package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.BoxVolume
import de.qabel.qabelbox.box.dto.BoxPath
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.DownloadSource
import de.qabel.qabelbox.box.dto.UploadSource
import rx.Observable

class BoxFileBrowserUseCase(private val volume: BoxVolume) : FileBrowserUseCase {
    override fun upload(path: BoxPath.File, source: UploadSource): Observable<Unit> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun download(path: BoxPath.File): Observable<DownloadSource> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(path: BoxPath): Observable<Unit> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun list(path: BoxPath.Folder): Observable<BrowserEntry> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createFolder(path: BoxPath.Folder): Observable<Unit> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
