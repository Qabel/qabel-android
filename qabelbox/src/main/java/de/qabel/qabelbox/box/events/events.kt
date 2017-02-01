package de.qabel.qabelbox.box.events

import de.qabel.box.storage.dto.BoxPath
import de.qabel.client.box.interactor.FileOperationState
import de.qabel.core.event.Event

interface BoxBackgroundEvent : Event
data class FileUploadEvent(val operation: FileOperationState) : BoxBackgroundEvent
data class FileDownloadEvent(val operation: FileOperationState) : BoxBackgroundEvent
data class BoxPathEvent(val boxPath: BoxPath.FolderLike, val complete: Boolean) : BoxBackgroundEvent
