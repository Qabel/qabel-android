package de.qabel.qabelbox.box.events

import de.qabel.core.event.Event
import de.qabel.qabelbox.box.dto.FileOperationState

data class FileUploadEvent(val operation: FileOperationState) : Event
data class FileDownloadEvent(val operation : FileOperationState) : Event
