package de.qabel.qabelbox.box.views

import de.qabel.qabelbox.box.provider.DocumentId

interface FileBrowserView: FileListingView {
    fun open(documentId: DocumentId)
    fun share(documentId: DocumentId)
    fun export(documentId: DocumentId)
}

