package de.qabel.box.storage

import de.qabel.core.crypto.QblECKeyPair

class AndroidBoxVolume(config: BoxVolumeConfig, keyPair: QblECKeyPair) : BoxVolumeImpl(config, keyPair) {

    override fun loadDriver() {
        Class.forName("org.sqldroid.SQLDroidDriver")
    }
}
