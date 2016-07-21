package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.*
import de.qabel.box.storage.exceptions.QblStorageNotFound
import de.qabel.core.config.Identity
import de.qabel.qabelbox.box.dto.*
import de.qabel.qabelbox.box.provider.DocumentId
import rx.Observable
import rx.lang.kotlin.observable
import rx.lang.kotlin.toSingletonObservable
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import kotlin.concurrent.thread

class BoxFileBrowserUseCase(identity: Identity,
                            readBackend: StorageReadBackend,
                            writeBackend: StorageWriteBackend,
                            deviceId: ByteArray,
                            tempDir: File) : FileBrowserUseCase {

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
        val nav = navigateTo(path.parent)
        val file = nav.listFiles().find { it.name == path.name }
        if (file != null) {
            it.onNext(BrowserEntry.File(file.name, file.size, Date(file.mtime)))
        } else {
            val folder = nav.listFolders().find { it.name == path.name }
            if (folder != null) {
                it.onNext(BrowserEntry.Folder(path.name))
            } else {
                it.onError(FileNotFoundException("File or folder not found"))
            }
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

    override fun delete(path: BoxPath): Observable<Unit> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun list(path: BoxPath.FolderLike): Observable<List<BrowserEntry>> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
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
