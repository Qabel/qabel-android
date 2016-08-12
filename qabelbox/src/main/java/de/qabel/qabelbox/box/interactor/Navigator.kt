package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.BoxNavigation
import de.qabel.qabelbox.box.dto.BoxPath

interface Navigator {
    val root: BoxNavigation
    val key: String
    val prefix: String

    fun navigateTo(path: BoxPath, action: (BoxPath, BoxNavigation) -> Unit = { a, b -> }):
            BoxNavigation
}
