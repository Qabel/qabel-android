package de.qabel.qabelbox.box.views

import de.qabel.qabelbox.box.provider.DocumentId

interface FolderChooserView: FileListingView {
    fun finish(documentId: DocumentId)

}

