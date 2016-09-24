package de.qabel.qabelbox.box.interactor

import de.qabel.qabelbox.box.dto.*
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.toDownloadSource
import rx.Observable
import rx.Single
import rx.lang.kotlin.toSingletonObservable
import java.io.FileNotFoundException
import java.util.*
import javax.inject.Inject

class MockFileBrowser @Inject constructor(): FileBrowser {

    private val root = DocumentId("identiy", "prefix", BoxPath.Root)

    override fun asDocumentId(path: BoxPath): Observable<DocumentId> =
            root.copy(path = path).toSingletonObservable()

    val storage = mutableListOf<Pair<BoxPath, ByteArray?>>()

    override fun createFolder(path: BoxPath.FolderLike): Observable<Unit> {
        if (path !is BoxPath.Root) {
            storage += Pair(path, null)
        }
        return Unit.toSingletonObservable()
    }

    override fun query(path: BoxPath): Single<BrowserEntry> {
        val res = storage.find { it.first == path } ?: throw IllegalArgumentException("File not found")
        val content = res.second
        if (content == null) {
            return BrowserEntry.Folder(path.name).toSingletonObservable<BrowserEntry>().toSingle()
        } else {
            return BrowserEntry.File(res.first.name, content.size.toLong(), Date())
                    .toSingletonObservable<BrowserEntry>().toSingle()
        }
    }
    override fun download(path: BoxPath.File): Single<DownloadSource> {
        for ((filepath, content) in storage) {
            if (filepath == path) {
                content ?: throw IllegalArgumentException("Not a file")
                return content.toDownloadSource(
                        BrowserEntry.File(
                                path.name,
                                storage.find { it.first == path }?.second?.size?.toLong()
                                    ?: throw IllegalArgumentException("File not found"),
                                Date())
                ).toSingletonObservable().toSingle()
            }
        }
        return Single.error<DownloadSource>(FileNotFoundException("File not found"))
    }

    override fun delete(path: BoxPath): Observable<Unit> {
        if (path is BoxPath.File) {
            storage.forEachIndexed { i, pair ->
                if (path == pair.first) {
                    storage.removeAt(i)
                }
            }
        } else if (path is BoxPath.Folder) {
            TODO()
        }
        return Unit.toSingletonObservable()
    }

    override fun list(path: BoxPath.FolderLike): Observable<List<BrowserEntry>> {
        return storage.filter { it.first.parent == path }.map { pair ->
            val (filepath, content) = pair
            when (filepath) {
                is BoxPath.File -> {
                    content ?: throw IllegalArgumentException("No content")
                    BrowserEntry.File(filepath.name, content.size.toLong(), Date())
                }
                is BoxPath.FolderLike -> BrowserEntry.Folder(filepath.name)
            }
        }.toSingletonObservable()
    }

    override fun upload(path: BoxPath.File, source: UploadSource): Observable<Unit> {
        storage += Pair(path, source.source.readBytes())
        return Unit.toSingletonObservable()
    }
}

