package de.qabel.qabelbox.box.provider

import de.qabel.box.storage.dto.BoxPath

data class DocumentId(val identityKey: String, val prefix: String, val path: BoxPath) {

    val pathString: String
        get() = path.parent.toList().joinToString(PATH_SEPARATOR)

    override fun toString(): String {
        return identityKey + BoxProvider.DOCID_SEPARATOR +
                prefix + BoxProvider.DOCID_SEPARATOR +
                pathString + PATH_SEPARATOR +
                if (path is BoxPath.FolderLike && path.name != "") {
                    path.name + PATH_SEPARATOR
                } else { path.name }
    }

    companion object {

        private val PATH_SEPARATOR = "/"
    }

}
