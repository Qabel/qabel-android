package de.qabel.qabelbox.box.backends

import de.qabel.box.storage.StorageDownload
import de.qabel.box.storage.StorageReadBackend
import de.qabel.box.storage.StorageWriteBackend
import de.qabel.box.storage.UnmodifiedException
import de.qabel.box.storage.exceptions.QblStorageNotFound
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*

class MockStorageBackend : StorageReadBackend, StorageWriteBackend {
    companion object {
        const val BASE = "local://"
    }

    val storage = mutableMapOf<String, Pair<String, ByteArray>>()


    override fun getUrl(meta: String) = BASE + meta

    override fun download(name: String) = download(name, null)


    override fun download(name: String, ifModifiedVersion: String?): StorageDownload {
        val (hash, content) = storage[name] ?: throw QblStorageNotFound("$name is not there")
        if (hash == ifModifiedVersion) {
            throw UnmodifiedException()
        }
        return StorageDownload(ByteArrayInputStream(content), hash, content.size.toLong())
    }

    override fun upload(name: String, content: InputStream): Long {
        val time = Date().time
        storage[name] = Pair(time.toString(), content.readBytes())
        return time
    }

    override fun delete(name: String) {
        storage.remove(name)
    }

}

