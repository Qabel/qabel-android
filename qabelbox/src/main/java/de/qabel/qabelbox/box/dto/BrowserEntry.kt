package de.qabel.qabelbox.box.dto

import org.apache.commons.lang3.builder.HashCodeBuilder
import java.util.*

sealed  class BrowserEntry(val name: String) {
    class File(name: String, val size: Long, val mTime: Date) : BrowserEntry(name) {
        override fun toString(): String {
            return "File($name, $size, $mTime)"
        }

        override fun equals(other: Any?): Boolean = when (other) {
            is File -> name == other.name && size == other.size && mTime == other.mTime
            else -> false
        }

        override fun hashCode(): Int{
            return HashCodeBuilder().append(size).append(mTime).build()
        }
    }
    class Folder(name: String) : BrowserEntry(name) {
        override fun toString(): String {
            return "Folder($name)"
        }
        override fun equals(other: Any?): Boolean = when (other) {
            is Folder -> name == other.name
            else -> false
        }

        override fun hashCode(): Int{
            return name.hashCode()
        }
    }
}



