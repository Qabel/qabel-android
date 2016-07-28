package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.*
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.box.storage.exceptions.QblStorageNotFound
import de.qabel.box.storage.jdbc.DirectoryMetadataDatabase
import de.qabel.box.storage.jdbc.JdbcDirectoryMetadataFactory
import de.qabel.core.config.Identity
import de.qabel.core.repositories.AndroidVersionAdapter
import de.qabel.qabelbox.box.dto.*
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.toEntry
import rx.Observable
import rx.lang.kotlin.observable
import rx.lang.kotlin.toSingletonObservable
import java.io.File
import java.io.FileNotFoundException
import java.sql.Connection
import javax.inject.Inject
import kotlin.concurrent.thread

class BoxFileBrowser @Inject constructor(identity: Identity,
                                         readBackend: StorageReadBackend,
                                         writeBackend: StorageWriteBackend,
                                         deviceId: ByteArray,
                                         tempDir: File) : FileBrowser {

    private val volume: BoxVolume
    private val prefix = identity.prefixes.first()
    private val key = identity.keyIdentifier

    init {
        val dataBaseFactory: (Connection) -> DirectoryMetadataDatabase = { connection ->
            DirectoryMetadataDatabase(connection, AndroidVersionAdapter(connection))
        }
        volume = AndroidBoxVolume(BoxVolumeConfig(
                prefix,
                deviceId,
                readBackend,
                writeBackend,
                "Blake2b",
                tempDir,
                directoryMetadataFactoryFactory = { tempDir, deviceId ->
                    JdbcDirectoryMetadataFactory(tempDir, deviceId, dataBaseFactory)
                }), identity.primaryKeyPair)
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
        thread {
            if (path is BoxPath.Root) {
                subscriber.onNext(BrowserEntry.Folder(""))
                return@thread
            }
            val boxObject = try {
                queryObject(path)
            } catch (e: FileNotFoundException) {
                subscriber.onError(e)
                return@thread
            }
            val entry = boxObject.toEntry()
            if (entry  == null) {
                subscriber.onError(FileNotFoundException("File or Folder ${path.name} not found"))
                return@thread
            }
            subscriber.onNext(entry)
        }
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
        thread {
            recursiveCreateFolder(path.parent).upload(path.name, source.source, source.entry.size)
            it.onNext(Unit)
            it.onCompleted()
        }
    }

    override fun download(path: BoxPath.File): Observable<DownloadSource> =
        observable { subscriber ->
            thread {
                query(path).subscribe { entry ->
                    if (entry is BrowserEntry.File) {
                        subscriber.onNext(DownloadSource(entry, navigateTo(path.parent).download(path.name)))
                    } else {
                        subscriber.onError(FileNotFoundException("Not a file"))
                    }
                }
            }
    }

    override fun delete(path: BoxPath): Observable<Unit> = observable {
        subscriber ->
        thread {
            try {
                val nav = navigateTo(path.parent)
                when (path) {
                    is BoxPath.Folder -> nav.getFolder(path.name).let { nav.delete(it) }
                    is BoxPath.File -> nav.getFile(path.name).let { nav.delete(it) }
                }
            } catch (e: QblStorageNotFound) {
            } catch (e: QblStorageException) {
                subscriber.onError(e)
                return@thread
            }
            subscriber.onNext(Unit)
        }
    }

    override fun list(path: BoxPath.FolderLike): Observable<List<BrowserEntry>> = observable {
        subscriber ->
        thread {
            val nav = try {
                navigateTo(path)
            } catch (e: QblStorageException) {
                subscriber.onError(e)
                return@thread
            }
            nav.reloadMetadata()
            val entries = nav.listFiles() + nav.listFolders()
            subscriber.onNext(entries.map { it.toEntry() }.filterNotNull())
        }
    }

    override fun createFolder(path: BoxPath.FolderLike): Observable<Unit> =
        observable { subscriber ->
            thread {
                if (path !is BoxPath.Root) {
                    try {
                        recursiveCreateFolder(path)
                        subscriber.onNext(Unit)
                    } catch (e: Exception) {
                        subscriber.onError(e)
                    }
                }
            }
    }

    private fun navigateTo(path: BoxPath, action: (BoxPath, BoxNavigation) -> (Unit) = {a,b -> }):
            BoxNavigation =
        if (path is BoxPath.Root || path.name == "") {
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
