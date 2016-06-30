package de.qabel.qabelbox.dto

import java.util.*

sealed  class BrowserEntry(val name: String) {
    class File(name: String, val size: Long, val mTime: Date) : BrowserEntry(name)
    class Folder(name: String) : BrowserEntry(name)
}



