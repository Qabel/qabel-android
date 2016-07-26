package de.qabel.qabelbox.box.dto

import de.qabel.qabelbox.box.provider.DocumentId

data class ProviderDownload(val documentId: DocumentId,
                            val source: DownloadSource)

