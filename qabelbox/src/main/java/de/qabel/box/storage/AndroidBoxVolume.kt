package de.qabel.box.storage

import de.qabel.box.storage.jdbc.JdbcFileMetadataFactory
import de.qabel.core.crypto.QblECKeyPair
import de.qabel.core.repositories.AndroidVersionAdapter

class AndroidBoxVolume(config: BoxVolumeConfig, keyPair: QblECKeyPair) : BoxVolumeImpl(config, keyPair) {

    companion object {
        init {
            JdbcFileMetadataFactory.versionAdapterFactory = ::AndroidVersionAdapter
        }
    }

    override fun loadDriver() {
        Class.forName("org.sqldroid.SQLDroidDriver")
    }
}
