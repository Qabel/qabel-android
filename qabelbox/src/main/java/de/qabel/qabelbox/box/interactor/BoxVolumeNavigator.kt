package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.*
import de.qabel.box.storage.exceptions.QblStorageNotFound
import de.qabel.box.storage.dto.BoxPath
import de.qabel.box.storage.local.LocalStorage
import de.qabel.core.logging.QabelLog
import java.io.FileNotFoundException
import javax.inject.Inject

class BoxVolumeNavigator @Inject constructor(
        keyAndPrefix: BoxReadFileBrowser.KeyAndPrefix,
        private val volume: BoxVolume,
        private val localStorage: LocalStorage) : VolumeNavigator, QabelLog {

    override val key = keyAndPrefix.publicKey
    override val prefix = keyAndPrefix.prefix

    private val navigationFactory by lazy {
        FolderNavigationFactory(localRoot ?: root, volume.config)
    }
    override val rootBoxFolder = BoxFolder(volume.config.rootRef, "root", volume.config.deviceId)

    override val root: IndexNavigation by lazy {
        (try {
            debug("Initialize Root navigation!")
            volume.navigate()
        } catch (e: QblStorageNotFound) {
            debug("Failed create Index!")
            volume.createIndex("qabel", prefix)
            volume.navigate()
        }).apply { localStorage.storeDirectoryMetadata(BoxPath.Root, rootBoxFolder, metadata, volume.config.prefix) }
    }

    val localRoot: IndexNavigation?
        get() = localStorage.getDirectoryMetadata(volume, BoxPath.Root, rootBoxFolder)?.let {
            volume.loadIndex(it)
        }

    override fun navigateFastTo(path: BoxPath.FolderLike, action: (BoxPath, BoxNavigation) -> Unit): BoxNavigation? =
            if (path is BoxPath.Root || path.name == "") {
                localRoot ?: root
            } else {
                debug("Navigate fast $path")
                val parent = navigateFastTo(path.parent, action)
                debug("Navigate fast to child $path")
                parent?.let {
                    println("DO ACTION NAV $path")
                    action(path, parent)
                    val targetFolder = parent.getFolder(path.name)
                    if (path is BoxPath.Folder) {
                        localStorage.getDirectoryMetadata(volume, path, targetFolder)?.let {
                            return navigationFactory.fromDirectoryMetadata(path, it, targetFolder)
                        }
                    }
                    null
                }
            }

    override fun navigateMixedTo(path: BoxPath.FolderLike, action: (BoxPath, BoxNavigation) -> Unit): BoxNavigation =
            if (path is BoxPath.Root || path.name == "") {
                localRoot ?: root
            } else {
                debug("Navigate fast $path")
                val parent = navigateFastTo(path.parent, action) ?: navigateTo(path.parent, action)
                debug("Navigate fast to child $path")
                println("DO ACTION NAV $path")
                action(path, parent)
                val targetFolder = parent.getFolder(path.name)
                localStorage.getDirectoryMetadata(volume, path, targetFolder)?.let {
                    return navigationFactory.fromDirectoryMetadata(path, it, targetFolder)
                } ?: parent.navigate(targetFolder)
            }

    override fun navigateTo(path: BoxPath.FolderLike, action: (BoxPath, BoxNavigation) -> Unit): BoxNavigation =
            if (path is BoxPath.Root || path.name == "") {
                debug("Refresh Root $path")
                root.apply {
                    refresh()
                    localStorage.storeDirectoryMetadata(path, rootBoxFolder, metadata, volume.config.prefix)
                }
            } else {
                debug("Navigate $path")
                val parent = navigateTo(path.parent, action)
                if (parent !== root) {
                    debug("Parent refresh $path")
                    parent.refresh()
                }
                action(path, parent)
                debug("Navigate to child $path")
                val targetChild = parent.getFolder(path.name)
                parent.navigate(targetChild).apply {
                    val targetPath = path as BoxPath.Folder
                    localStorage.storeDirectoryMetadata(targetPath, targetChild, metadata, volume.config.prefix)
                }
            }

    override fun queryObjectAndNav(path: BoxPath): Pair<BoxObject, BoxNavigation> {
        with(navigateMixedTo(path.parent)) {
            return Pair(listFiles().find { it.name == path.name } ?:
                    listFolders().find { it.name == path.name } ?:
                    throw FileNotFoundException("Not found: ${path}"),
                    this)
        }
    }

}
