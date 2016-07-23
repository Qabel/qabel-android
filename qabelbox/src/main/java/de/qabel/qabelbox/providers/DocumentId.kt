package de.qabel.qabelbox.providers

import java.util.Arrays

import de.qabel.core.StringUtils

class DocumentId(val identityKey: String, val prefix: String, val path: Array<String>, val fileName: String) {

    val pathString: String
        get() {
            if (path.size == 0) {
                return PATH_SEPARATOR
            }
            return StringUtils.join(PATH_SEPARATOR, path)
        }

    val filePath: String
        get() {
            val parts = Arrays.copyOf(path, path.size + 1)
            parts[parts.size - 1] = fileName
            return StringUtils.join(PATH_SEPARATOR, parts)
        }

    override fun toString(): String {
        return identityKey + BoxProvider.DOCID_SEPARATOR +
                prefix + BoxProvider.DOCID_SEPARATOR +
                pathString + PATH_SEPARATOR + fileName
    }

    companion object {

        private val PATH_SEPARATOR = "/"
    }

}
