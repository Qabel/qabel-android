package de.qabel.qabelbox.box.presenters

import de.qabel.box.storage.dto.BoxPath
import de.qabel.qabelbox.box.views.FileUploadView
import de.qabel.qabelbox.identity.interactor.ReadOnlyIdentityInteractor

class ExternalFileUploadPresenter(val view: FileUploadView,
                                  val identityInteractor: ReadOnlyIdentityInteractor): FileUploadPresenter {

    override val defaultPath: BoxPath = BoxPath.Root / "public"

    override val availableIdentities: List<FileUploadPresenter.IdentitySelection>
        = identityInteractor.getIdentities().toBlocking().value().identities.map {
            FileUploadPresenter.IdentitySelection(it)
        }

}

