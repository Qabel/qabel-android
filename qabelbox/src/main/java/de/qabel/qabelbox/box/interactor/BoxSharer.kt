package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.BoxFile
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.repository.entities.ChatDropMessage
import de.qabel.core.service.ChatService
import de.qabel.qabelbox.box.dto.BoxPath
import rx.Observable
import rx.schedulers.Schedulers
import java.io.FileNotFoundException
import javax.inject.Inject

class BoxSharer @Inject constructor(private val volumeNavigator: VolumeNavigator,
                                    private val chatService: ChatService,
                                    private val owner: Identity): Sharer {

    override fun sendFileShare(contact: Contact, path: BoxPath): Observable<Unit> = Observable.just(Unit).map {
        val (boxObject, nav) = volumeNavigator.queryObjectAndNav(path)
        if (nav.getSharesOf(boxObject).find { it.recipient == contact.keyIdentifier } != null) {
        }
        if (boxObject is BoxFile) {
            nav.share(owner.ecPublicKey, boxObject, contact.keyIdentifier)
        } else {
            throw FileNotFoundException("Not a file")
        }
    }.map {
        ChatDropMessage(contact.id, owner.id,
            ChatDropMessage.Direction.OUTGOING, ChatDropMessage.Status.PENDING,
            ChatDropMessage.MessageType.SHARE_NOTIFICATION,
            ChatDropMessage.MessagePayload.ShareMessage("", it.url, it.key),
            System.currentTimeMillis())
    }.map {
        chatService.sendMessage(it)
    }.subscribeOn(Schedulers.io())
}
