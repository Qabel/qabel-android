package de.qabel.qabelbox.box.dto

import de.qabel.client.box.documentId.DocumentId
import de.qabel.client.box.interactor.UploadSource

data class ProviderUpload(val documentId: DocumentId, val source: UploadSource)

