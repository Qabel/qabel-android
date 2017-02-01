package de.qabel.qabelbox.box.views

import de.qabel.client.box.documentId.DocumentId

interface FolderChooserView: FileListingView {

    fun finish(documentId: DocumentId)

}

