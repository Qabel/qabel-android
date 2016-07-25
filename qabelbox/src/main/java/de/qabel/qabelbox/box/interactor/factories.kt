package de.qabel.qabelbox.box.interactor

import de.qabel.desktop.repository.IdentityRepository
import de.qabel.qabelbox.box.backends.BoxHttpStorageBackend
import de.qabel.qabelbox.box.dto.VolumeRoot
import de.qabel.qabelbox.box.provider.toDocumentId
import de.qabel.qabelbox.storage.server.BlockServer
import java.io.File

fun makeFileBrowserFactory(identityRepository: IdentityRepository,
                           deviceId: ByteArray,
                           tempDir: File,
                           androidBlockServer: BlockServer):
        (VolumeRoot) -> FileBrowser {
    return fun(volumeRoot: VolumeRoot): FileBrowser {
        val docId = volumeRoot.documentID.toDocumentId()
        val key = docId.identityKey
        val identity = identityRepository.find(key)
        val backend = BoxHttpStorageBackend(androidBlockServer, docId.prefix)
        return BoxFileBrowser(identity, backend, backend, deviceId, tempDir)
    }
}

