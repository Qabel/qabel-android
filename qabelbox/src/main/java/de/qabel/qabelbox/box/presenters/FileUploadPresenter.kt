package de.qabel.qabelbox.box.presenters

import de.qabel.box.storage.dto.BoxPath
import de.qabel.core.config.Identity
import de.qabel.qabelbox.contacts.dto.EntitySelection

interface FileUploadPresenter {
    val availableIdentities: List<EntitySelection>
    val defaultPath: BoxPath

    fun confirm()

}

