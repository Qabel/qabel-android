package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.BoxNavigation
import de.qabel.box.storage.BoxObject
import de.qabel.qabelbox.box.dto.BoxPath

interface VolumeNavigator {
    val root: BoxNavigation
    val key: String
    val prefix: String

    fun navigateTo(path: BoxPath, action: (BoxPath, BoxNavigation) -> Unit = { a, b -> }):
            BoxNavigation

    fun queryObjectAndNav(path: BoxPath): Pair<BoxObject, BoxNavigation>

}
