package de.qabel.qabelbox.box.dto

sealed class BoxPath() {

    abstract val name: String

    abstract val parent: BoxPath

    class File(override val name: String, override val parent: BoxPath): BoxPath()


    abstract class FolderLike(): BoxPath() {
        fun div(name: String) = Folder(name, this)
        fun div(path: File) = File(path.name, this)
        fun div(path: Folder) = Folder(path.name, this)

        fun file(name: String) = File(name, this)
    }

    class Folder(override val name: String, override val parent: BoxPath): FolderLike()

    object Root : FolderLike() {
        override val name: String
            get() = ""
        override val parent: BoxPath
            get() = this

    }

    fun dec() = parent

    open fun equals(other: BoxPath): Boolean = (name == other.name) && parent.equals(other.parent)

}

