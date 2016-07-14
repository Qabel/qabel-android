package de.qabel.qabelbox.box.provider

import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.qabelbox.box.dto.BoxPath
import java.io.FileNotFoundException
import java.util.ArrayList
import java.util.Arrays
import javax.inject.Inject


/**
 * Document IDs are built like this:
 * public-key::::prefix::::/filepath
 */
class DocumentIdParser @Inject constructor(){

    @Throws(FileNotFoundException::class)
    fun getIdentity(documentId: String): String {
        val split = documentId.split(BoxProvider.DOCID_SEPARATOR.toRegex(), MAX_TOKEN_SPLITS).toTypedArray()
        if (split.size > 0 && split[0].length > 0) {
            return split[0]
        }
        throw FileNotFoundException("Could not find identity in document id")
    }


    @Throws(FileNotFoundException::class)
    fun getPrefix(documentId: String): String {
        val split = documentId.split(BoxProvider.DOCID_SEPARATOR.toRegex(), MAX_TOKEN_SPLITS).toTypedArray()
        if (split.size > 1 && split[1].length > 0) {
            return split[1]
        }
        throw FileNotFoundException("Could not find volume prefix in document id")
    }


    @Throws(FileNotFoundException::class)
    fun getFilePath(documentId: String): String {
        val split = documentId.split(BoxProvider.DOCID_SEPARATOR.toRegex(), MAX_TOKEN_SPLITS).toTypedArray()
        if (split.size > 2 && split[2].length > 0) {
            return split[2]
        }
        throw FileNotFoundException("Could not find file path in document id")
    }

    @Throws(FileNotFoundException::class)
    fun getPath(documentId: String): String {
        var filepath = getFilePath(documentId)
        filepath = filepath.substring(0, filepath.lastIndexOf('/') + 1)
        // TODO: Workaround for wrong formatted document IDs
        if (filepath.startsWith("//")) {
            return filepath.substring(1, filepath.length)
        }
        return filepath
    }

    fun splitPath(filePath: String): List<String> {

        return ArrayList(Arrays.asList(*filePath.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
    }

    @Throws(FileNotFoundException::class)
    fun getBaseName(documentID: String): String {
        val filepath = getFilePath(documentID)
        return filepath.substring(filepath.lastIndexOf('/') + 1, filepath.length)
    }


    fun buildId(identity: String, prefix: String?, filePath: String?): String {
        if (prefix != null && filePath != null) {
            return identity + BoxProvider.DOCID_SEPARATOR + prefix + BoxProvider.DOCID_SEPARATOR + filePath
        } else if (prefix != null) {
            return identity + BoxProvider.DOCID_SEPARATOR + prefix
        } else {
            return identity
        }
    }

    @Throws(QblStorageException::class)
    fun parse(documentId: String): DocumentId {
        val parts = documentId.split(BoxProvider.DOCID_SEPARATOR.toRegex(), MAX_TOKEN_SPLITS).toTypedArray()
        if (parts.size != 3) {
            throw QblStorageException("Invalid documentId: " + documentId)
        }
        val (identityKey, prefix, completePath) = parts
        val pathParts = completePath.split("/".toRegex()).dropLastWhile { it.isEmpty() }
        if (pathParts.size == 0) {
            return DocumentId(identityKey, prefix, BoxPath.Root)
        }
        val path = if (documentId.last() == '/') {
            pathParts.fold<String, BoxPath>(BoxPath.Root) { path, part ->
                BoxPath.Folder(part, path)
            }
        } else {
            pathParts.fold<String, BoxPath>(BoxPath.Root) { path, part ->
                BoxPath.File(part, path)
            }
        }
        return DocumentId(identityKey, prefix, path)
    }

    companion object {

        val MAX_TOKEN_SPLITS = 3
    }
}
