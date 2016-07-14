package de.qabel.qabelbox.box.dto

import de.qabel.qabelbox.box.provider.DocumentId

data class ProviderUpload(val documentId: DocumentId, val source: UploadSource)

