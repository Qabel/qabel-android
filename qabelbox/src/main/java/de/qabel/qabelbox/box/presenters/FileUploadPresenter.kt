package de.qabel.qabelbox.box.presenters

import de.qabel.box.storage.dto.BoxPath
import de.qabel.core.config.Identity

interface FileUploadPresenter {
    val availableIdentities: List<IdentitySelection>
    val defaultPath: BoxPath

    fun confirm()

    data class IdentitySelection(val alias: String, val keyId: String) {
        constructor(identity: Identity): this(identity.alias, identity.keyIdentifier)

        override fun toString(): String = alias
    }

}

