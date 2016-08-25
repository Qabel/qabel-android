package de.qabel.qabelbox.box.interactor

import de.qabel.box.storage.BoxExternalReference
import de.qabel.box.storage.BoxFile
import de.qabel.box.storage.StorageReadBackend
import de.qabel.chat.repository.entities.ChatDropMessage
import de.qabel.chat.service.ChatService
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.core.config.SymmetricKey
import de.qabel.qabelbox.box.dto.BoxPath
import rx.Observable
import rx.schedulers.Schedulers
import java.io.FileNotFoundException
import java.net.URI
import javax.inject.Inject

class BoxSharer @Inject constructor(private val volumeNavigator: VolumeNavigator,
                                    private val chatService: ChatService,
                                    private val owner: Identity,
                                    private val readBackend: StorageReadBackend) : Sharer {

    override fun sendFileShare(contact: Contact, path: BoxPath): Observable<Unit> = Observable.just(Unit).map {
        val (boxObject, nav) = volumeNavigator.queryObjectAndNav(path)
        (if (boxObject !is BoxFile) {
            throw FileNotFoundException("Not a file")
        } else {
            nav.getSharesOf(boxObject).find { it.recipient == contact.keyIdentifier }?.let {
                BoxExternalReference(
                        false,
                        readBackend.getUrl(boxObject.meta),
                        boxObject.name,
                        owner.ecPublicKey,
                        boxObject.metakey)
            } ?: nav.share(owner.ecPublicKey, boxObject, contact.keyIdentifier)
        }).let {
            ChatDropMessage(contact.id, owner.id,
                    ChatDropMessage.Direction.OUTGOING, ChatDropMessage.Status.PENDING,
                    ChatDropMessage.MessageType.SHARE_NOTIFICATION,
                    ChatDropMessage.MessagePayload.ShareMessage("", URI(it.url), SymmetricKey.Factory.fromBytes(it.key), boxObject.name, boxObject.size),
                    System.currentTimeMillis()).let {
                chatService.sendMessage(it)
            }
        }
    }.subscribeOn(Schedulers.io())
}
