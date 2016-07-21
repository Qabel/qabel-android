package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.BoxVolume
import de.qabel.qabelbox.box.dto.*
import de.qabel.qabelbox.box.provider.DocumentId
import rx.Observable

class BoxFileBrowserUseCase(private val volume: VolumeRoot) : FileBrowserUseCase {
    override fun asDocumentId(path: BoxPath): Observable<DocumentId> {
        TODO()
    }

    override fun query(path: BoxPath): Observable<BrowserEntry> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun upload(path: BoxPath.File, source: UploadSource): Observable<Unit> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun download(path: BoxPath.File): Observable<DownloadSource> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(path: BoxPath): Observable<Unit> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun list(path: BoxPath.FolderLike): Observable<List<BrowserEntry>> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createFolder(path: BoxPath.Folder): Observable<Unit> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
