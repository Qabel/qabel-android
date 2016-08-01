package de.qabel.qabelbox.box.interactor

import de.qabel.core.repository.IdentityRepository
import de.qabel.qabelbox.box.dto.BoxPath
import de.qabel.qabelbox.box.dto.VolumeRoot
import de.qabel.qabelbox.box.provider.DocumentId
import java.io.FileNotFoundException

class BoxVolumeManager (private val identityRepository: IdentityRepository,
                        private val fileBrowserFactory: (VolumeRoot) -> FileBrowser):
        VolumeManager {

    override val roots: List<VolumeRoot>
        get() = identityRepository.findAll().identities.map {
            val docId = DocumentId(it.keyIdentifier, it.prefixes.first(), BoxPath.Root)
            VolumeRoot(docId.toString().dropLast(1), docId.toString(), it.alias)
        }

    override fun fileBrowser(rootID: String) =
            fileBrowserFactory(roots.find { it.documentID == rootID }
                    ?: throw FileNotFoundException("No filebrowser for root id found: " + rootID))

}

