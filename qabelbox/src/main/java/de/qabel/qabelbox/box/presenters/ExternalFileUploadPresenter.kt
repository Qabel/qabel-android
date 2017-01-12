package de.qabel.qabelbox.box.presenters

import de.qabel.box.storage.dto.BoxPath
import de.qabel.client.box.documentId.DocumentId
import de.qabel.core.config.Prefix
import de.qabel.qabelbox.box.views.FileUploadView
import de.qabel.qabelbox.identity.interactor.ReadOnlyIdentityInteractor
import javax.inject.Inject

class ExternalFileUploadPresenter @Inject constructor(val view: FileUploadView,
                                                      val identityInteractor: ReadOnlyIdentityInteractor): FileUploadPresenter {
    override fun confirm() {
        val identityKey = view.identity.keyId
        identityInteractor.getIdentity(identityKey).doOnSuccess {
            view.startUpload(DocumentId(identityKey,
                    it.prefixes.first { it.type == Prefix.TYPE.USER }.prefix,
                    view.path * view.filename))
        }.subscribe()
    }

    override val defaultPath: BoxPath = BoxPath.Root / "public"

    override val availableIdentities: List<FileUploadPresenter.IdentitySelection>
        = identityInteractor.getIdentities().toBlocking().value().identities.map {
            FileUploadPresenter.IdentitySelection(it)
        }.sortedBy { it.alias }

}

