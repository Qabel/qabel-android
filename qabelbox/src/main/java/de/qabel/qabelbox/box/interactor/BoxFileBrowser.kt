package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.BoxNavigation
import de.qabel.box.storage.BoxObject
import de.qabel.box.storage.BoxVolume
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.box.storage.exceptions.QblStorageNotFound
import de.qabel.qabelbox.box.dto.BoxPath
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.DownloadSource
import de.qabel.qabelbox.box.dto.UploadSource
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.toEntry
import rx.Observable
import rx.lang.kotlin.observable
import rx.lang.kotlin.toSingletonObservable
import rx.schedulers.Schedulers
import java.io.FileNotFoundException
import javax.inject.Inject

class BoxFileBrowser @Inject constructor(keyAndPrefix: KeyAndPrefix,
                                         private val volume: BoxVolume
                                         ) : FileBrowser, VolumeNavigator by BoxVolumeNavigator(keyAndPrefix, volume) {

    data class KeyAndPrefix(val publicKey: String, val prefix: String)

    override fun asDocumentId(path: BoxPath) = DocumentId(key, prefix, path).toSingletonObservable()


    override fun query(path: BoxPath): Observable<BrowserEntry> = observable<BrowserEntry> {
        subscriber ->
        if (path is BoxPath.Root) {
            subscriber.onNext(BrowserEntry.Folder(""))
            return@observable
        }
        val boxObject = try {
            queryObject(path)
        } catch (e: Throwable) {
            subscriber.onError(e)
            return@observable
        }
        val entry = boxObject.toEntry()
        if (entry  == null) {
            subscriber.onError(FileNotFoundException("File or Folder ${path.name} not found"))
            return@observable
        }
        subscriber.onNext(entry)
    }.subscribeOn(Schedulers.io())

    private fun queryObject(path: BoxPath): BoxObject =
           queryObjectAndNav(path).first

    override fun upload(path: BoxPath.File, source: UploadSource): Observable<Unit> = observable<Unit> {
        try {
            recursiveCreateFolder(path.parent).upload(path.name, source.source, source.entry.size)
            it.onNext(Unit)
        } catch (e: Throwable) {
            it.onError(e)
        }
        it.onCompleted()
    }.subscribeOn(Schedulers.io())

    override fun download(path: BoxPath.File): Observable<DownloadSource> =
        observable<DownloadSource> { subscriber ->
            query(path).subscribe({ entry ->
                if (entry is BrowserEntry.File) {
                    subscriber.onNext(DownloadSource(entry, navigateTo(path.parent).download(path.name)))
                } else {
                    subscriber.onError(FileNotFoundException("Not a file"))
                }
            }, { subscriber.onError(it) })
    }.subscribeOn(Schedulers.io())

    override fun delete(path: BoxPath): Observable<Unit> = observable<Unit> {
        subscriber ->
        try {
            val nav = navigateTo(path.parent)
            when (path) {
                is BoxPath.Folder -> nav.getFolder(path.name).let { nav.delete(it) }
                is BoxPath.File -> nav.getFile(path.name).let { nav.delete(it) }
            }
        } catch (e: QblStorageNotFound) {
        } catch (e: QblStorageException) {
            subscriber.onError(e)
            return@observable
        }
        subscriber.onNext(Unit)
    }.subscribeOn(Schedulers.io())

    override fun list(path: BoxPath.FolderLike): Observable<List<BrowserEntry>> =
            observable<List<BrowserEntry>> {
        subscriber ->
        val nav = try {
            navigateTo(path).apply { reloadMetadata() }
        } catch (e: QblStorageException) {
            subscriber.onError(e)
            return@observable
        }
        val entries = nav.listFolders().sortedBy { it.name } + nav.listFiles().sortedBy { it.name }
        subscriber.onNext(entries.map { it.toEntry() }.filterNotNull())
    }.subscribeOn(Schedulers.io())

    override fun createFolder(path: BoxPath.FolderLike): Observable<Unit> =
        observable<Unit> { subscriber ->
            try {
                recursiveCreateFolder(path)
                subscriber.onNext(Unit)
            } catch (e: Throwable) {
                subscriber.onError(e)
            }
    }.subscribeOn(Schedulers.io())

    private fun recursiveCreateFolder(path: BoxPath.FolderLike): BoxNavigation =
        navigateTo(path) { p, nav ->
            nav.listFolders().find { it.name == p.name } ?: nav.createFolder(p.name)
            nav.commitIfChanged()
        }
}
