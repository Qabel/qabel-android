package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.BoxNavigation
import de.qabel.qabelbox.box.dto.BoxPath
import javax.inject.Inject

class BoxNavigationInteractor @Inject constructor(volumeInteractor: VolumeInteractor)
: NavigationInteractor, VolumeInteractor by volumeInteractor {

    override fun navigateTo(path: BoxPath, action: (BoxPath, BoxNavigation) -> (Unit)):
            BoxNavigation =
            if (path is BoxPath.Root || path.name == "") {
                root
            } else {
                val parent = navigateTo(path.parent, action)
                action(path, parent)
                parent.navigate(path.name)
            }

}

