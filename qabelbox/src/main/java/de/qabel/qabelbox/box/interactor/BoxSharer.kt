package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.BoxFile
import de.qabel.box.storage.StorageReadBackend
import de.qabel.chat.repository.entities.ChatDropMessage
import de.qabel.chat.service.ChatService
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.qabelbox.box.dto.BoxPath
import rx.Observable
import java.io.FileNotFoundException
import javax.inject.Inject

class BoxSharer @Inject constructor(private val volumeNavigator: VolumeNavigator,
                                    private val chatService: ChatService,
                                    private val owner: Identity,
                                    private val readBackend: StorageReadBackend) : Sharer {

    override fun sendFileShare(contact: Contact, path: BoxPath): Observable<ChatDropMessage> {
        val (boxObject, nav) = volumeNavigator.queryObjectAndNav(path)
        if (boxObject !is BoxFile) {
            throw FileNotFoundException("Not a file")
        } else {
            return chatService.sendShareMessage(boxObject.name, owner, contact, boxObject, nav)
        }
    }
}
