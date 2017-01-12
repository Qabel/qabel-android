package de.qabel.qabelbox.box.views

import de.qabel.client.box.documentId.DocumentId

interface FileBrowserView: FileListingView {
    fun open(documentId: DocumentId)
    fun share(documentId: DocumentId)
    fun export(documentId: DocumentId)
}

