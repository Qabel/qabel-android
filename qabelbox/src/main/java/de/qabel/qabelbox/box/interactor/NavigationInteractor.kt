package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.BoxNavigation
import de.qabel.qabelbox.box.dto.BoxPath

interface NavigationInteractor {

    fun navigateTo(path: BoxPath,
                   action: (BoxPath, BoxNavigation) -> (Unit) = { a, b -> }): BoxNavigation
}

