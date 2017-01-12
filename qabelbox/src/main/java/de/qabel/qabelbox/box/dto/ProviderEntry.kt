package de.qabel.qabelbox.box.dto

import de.qabel.client.box.documentId.DocumentId
import de.qabel.client.box.interactor.BrowserEntry

data class ProviderEntry(val documentId: DocumentId, val entry: BrowserEntry)

