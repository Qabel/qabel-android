package de.qabel.qabelbox.dto

import java.util.*

data class FileEntry(val name: String, val mtime: Date, val size: Long, val type: EntryType)

enum class EntryType {
    FILE, FOLDER
}

