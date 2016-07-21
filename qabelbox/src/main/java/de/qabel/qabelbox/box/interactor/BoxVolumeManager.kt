package de.qabel.qabelbox.box.interactor

import de.qabel.desktop.repository.IdentityRepository
import de.qabel.qabelbox.box.dto.BoxPath
import de.qabel.qabelbox.box.dto.VolumeRoot
import de.qabel.qabelbox.box.provider.DocumentId
import java.io.FileNotFoundException
import javax.inject.Inject

class BoxVolumeManager @Inject constructor(
        private val identityRepository: IdentityRepository,
        private val fileBrowserFactory: (VolumeRoot) -> FileBrowserUseCase):
        VolumeManager {

    override val roots: List<VolumeRoot>
        get() = identityRepository.findAll().identities.map {
            val docId = DocumentId(it.keyIdentifier, it.prefixes.first(), BoxPath.Root)
            VolumeRoot(docId.toString().dropLast(1), docId.toString(), it.alias)
        }

    override fun fileBrowser(rootID: String) =
            fileBrowserFactory(roots.find { it.rootID == rootID }
                    ?: throw FileNotFoundException("No filebrowser for this root id found"))

}
