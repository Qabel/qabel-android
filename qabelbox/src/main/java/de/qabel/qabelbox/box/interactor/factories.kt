package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.StorageReadBackend
import de.qabel.box.storage.StorageWriteBackend
import de.qabel.desktop.repository.IdentityRepository
import de.qabel.qabelbox.box.dto.VolumeRoot
import de.qabel.qabelbox.box.provider.toDocumentId
import java.io.File

fun makeFileBrowserFactory(identityRepository: IdentityRepository,
                           deviceId: ByteArray,
                           readBackend: StorageReadBackend,
                           writeBackend: StorageWriteBackend,
                           tempDir: File):
        (VolumeRoot) -> FileBrowser {
    return fun(volumeRoot: VolumeRoot): FileBrowser {
        val key = volumeRoot.documentID.toDocumentId().identityKey
        val identity = identityRepository.find(key)
        return BoxFileBrowser(identity, readBackend, writeBackend, deviceId, tempDir)
    }
}

