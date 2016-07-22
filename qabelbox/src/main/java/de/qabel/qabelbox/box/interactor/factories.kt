package de.qabel.qabelbox.box.interactor

import de.qabel.desktop.repository.IdentityRepository
import de.qabel.qabelbox.box.backends.MockStorageBackend
import de.qabel.qabelbox.box.dto.VolumeRoot
import de.qabel.qabelbox.box.provider.toDocumentId

fun makeFileBrowserFactory(identityRepository: IdentityRepository):
        (VolumeRoot) -> FileBrowserUseCase {
    return fun(volumeRoot: VolumeRoot): FileBrowserUseCase {
        val key = volumeRoot.documentID.toDocumentId().identityKey
        val identity = identityRepository.find(key)
        val backend = MockStorageBackend()
        return BoxFileBrowserUseCase(identity, backend, backend, byteArrayOf(1), createTempDir())
    }
}

