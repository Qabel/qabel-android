package de.qabel.qabelbox.box.dto

import de.qabel.client.box.documentId.DocumentId
import de.qabel.client.box.interactor.DownloadSource

data class ProviderDownload(val documentId: DocumentId,
                            val source: DownloadSource)

