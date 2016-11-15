package de.qabel.qabelbox.box.presenters

import de.qabel.box.storage.dto.BoxPath
import de.qabel.core.config.Identity

interface FileUploadPresenter {
    val availableIdentities: List<IdentitySelection>
    val defaultPath: BoxPath

    fun confirm()

    data class IdentitySelection(val identity: Identity) {
        val alias: String = identity.alias
        val keyId: String = identity.keyIdentifier

        override fun toString(): String = alias
    }

}

