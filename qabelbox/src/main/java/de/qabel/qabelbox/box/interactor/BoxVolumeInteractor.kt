package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.BoxVolume
import de.qabel.box.storage.IndexNavigation
import de.qabel.box.storage.exceptions.QblStorageNotFound
import javax.inject.Inject

class BoxVolumeInteractor @Inject constructor(
        private val boxVolume: BoxVolume,
        keyAndPrefix: BoxFileBrowser.KeyAndPrefix
): VolumeInteractor {

    override val root: IndexNavigation by lazy {
        try {
            boxVolume.navigate()
        } catch (e: QblStorageNotFound) {
            boxVolume.createIndex("qabel", keyAndPrefix.prefix)
            boxVolume.navigate()
        }
    }
}

