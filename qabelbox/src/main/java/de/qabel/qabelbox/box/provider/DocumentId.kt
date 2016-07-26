package de.qabel.qabelbox.box.provider

import de.qabel.qabelbox.box.dto.BoxPath

data class DocumentId(val identityKey: String, val prefix: String, val path: BoxPath) {

    val pathString: String
        get() = path.parent.toList().joinToString(PATH_SEPARATOR)

    override fun toString(): String {
        return identityKey + BoxProvider.DOCID_SEPARATOR +
                prefix + BoxProvider.DOCID_SEPARATOR +
                pathString + PATH_SEPARATOR + path.name
    }

    companion object {

        private val PATH_SEPARATOR = "/"
    }

}
