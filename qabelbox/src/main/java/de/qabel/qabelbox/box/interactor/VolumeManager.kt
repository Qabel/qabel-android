package de.qabel.qabelbox.box.interactor

import de.qabel.qabelbox.box.dto.VolumeRoot

interface VolumeManager {

    val roots: List<VolumeRoot>

    fun readFileBrowser(rootID: String): ReadFileBrowser
    fun operationFileBrowser(rootID: String): OperationFileBrowser

}

