package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.*
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.box.storage.exceptions.QblStorageNotFound
import de.qabel.core.config.Identity
import de.qabel.qabelbox.box.dto.*
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.toEntry
import rx.Observable
import rx.lang.kotlin.observable
import rx.lang.kotlin.toSingletonObservable
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject

class BoxFileBrowser @Inject constructor(identity: Identity,
                                         readBackend: StorageReadBackend,
                                         writeBackend: StorageWriteBackend,
                                         deviceId: ByteArray,
                                         tempDir: File) : FileBrowser {

    private val volume: BoxVolume
    private val prefix = identity.prefixes.first()
    private val key = identity.keyIdentifier

    init {
        volume = BoxVolume(BoxVolumeConfig(
                prefix,
                deviceId,
                readBackend,
                writeBackend,
                "Blake2b", tempDir), identity.primaryKeyPair)
    }
    private val root: BoxNavigation by lazy {
        try {
            volume.navigate()
        } catch (e: QblStorageNotFound) {
            volume.createIndex("qabel", prefix)
            volume.navigate()
        }
    }


    override fun asDocumentId(path: BoxPath) = DocumentId(key, prefix, path).toSingletonObservable()


    override fun query(path: BoxPath): Observable<BrowserEntry> = observable {
        subscriber ->
        val boxObject = try {
            queryObject(path)
        } catch (e: FileNotFoundException) {
            subscriber.onError(e)
            return@observable
        }
        val entry = boxObject.toEntry()
        if (entry  == null) {
            subscriber.onError(FileNotFoundException("File or Folder ${path.name} not found"))
            return@observable
        }
        subscriber.onNext(entry)
    }

    private fun queryObject(path: BoxPath): BoxObject =
           queryObjectAndNav(path).first

    private fun queryObjectAndNav(path: BoxPath): Pair<BoxObject, BoxNavigation> {
        with(navigateTo(path.parent)) {
            return Pair(listFiles().find { it.name == path.name } ?:
                    listFolders().find { it.name == path.name } ?:
                    throw FileNotFoundException("Not found: ${path.name}"),
                    this)
        }
    }

    override fun upload(path: BoxPath.File, source: UploadSource): Observable<Unit> = observable {
        recursiveCreateFolder(path.parent).upload(path.name, source.source, source.entry.size)
        it.onNext(Unit)
        it.onCompleted()
    }

    override fun download(path: BoxPath.File): Observable<DownloadSource> =
        observable { subscriber ->
        query(path).subscribe { entry ->
            if (entry is BrowserEntry.File) {
                subscriber.onNext(DownloadSource(entry, navigateTo(path.parent).download(path.name)))
            } else {
                subscriber.onError(FileNotFoundException("Not a file"))
            }
        }
    }

    override fun delete(path: BoxPath): Observable<Unit> = observable {
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
    }

    override fun list(path: BoxPath.FolderLike): Observable<List<BrowserEntry>> = observable {
        subscriber ->
        val nav = try {
            navigateTo(path)
        } catch (e: QblStorageException) {
            subscriber.onError(e)
            return@observable
        }
        val entries = nav.listFiles() + nav.listFolders()
        subscriber.onNext(entries.map { it.toEntry() }.filterNotNull())
    }

    override fun createFolder(path: BoxPath.FolderLike): Observable<Unit> =
        observable { subscriber ->
            if (path !is BoxPath.Root) {
                try {
                    recursiveCreateFolder(path)
                    subscriber.onNext(Unit)
                } catch (e: Exception) {
                    subscriber.onError(e)
                }
            }
    }

    private fun navigateTo(path: BoxPath, action: (BoxPath, BoxNavigation) -> (Unit) = {a,b -> }):
            BoxNavigation =
        if (path is BoxPath.Root) {
            root
        } else {
            val parent = navigateTo(path.parent, action)
            action(path, parent)
            parent.navigate(path.name)
        }

    private fun recursiveCreateFolder(path: BoxPath.FolderLike): BoxNavigation =
        navigateTo(path) { p, nav ->
            nav.listFolders().find { it.name == p.name } ?: nav.createFolder(p.name)
            nav.commitIfChanged()
        }
}
