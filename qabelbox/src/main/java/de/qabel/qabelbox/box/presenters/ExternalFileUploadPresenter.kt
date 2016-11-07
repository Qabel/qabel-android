package de.qabel.qabelbox.box.presenters

import de.qabel.qabelbox.box.views.FileUploadView
import de.qabel.qabelbox.identity.interactor.ReadOnlyIdentityInteractor

class ExternalFileUploadPresenter(val view: FileUploadView, identityInteractor: ReadOnlyIdentityInteractor): FileUploadPresenter {

    override fun startUpload() {
    }

}

