package de.qabel.qabelbox.box.views

import android.os.Bundle
import de.qabel.box.storage.dto.BoxPath
import de.qabel.core.logging.QabelLog
import de.qabel.qabelbox.R
import de.qabel.qabelbox.base.CrashReportingActivity
import de.qabel.qabelbox.box.presenters.FileUploadPresenter
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.dagger.modules.ActivityModule
import de.qabel.qabelbox.dagger.modules.ExternalFileUploadModule
import kotlinx.android.synthetic.main.activity_external_upload.*
import javax.inject.Inject

class ExternalFileUploadActivity() : FileUploadView, CrashReportingActivity(), QabelLog {

    override lateinit var identity: FileUploadPresenter.IdentitySelection
    override var path: BoxPath = BoxPath.Root / "Upload"
    override var filename: String = ""

    @Inject
    lateinit var presenter: FileUploadPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationComponent.plus(ActivityModule(this))
                .plus(ExternalFileUploadModule((this)))
                .inject(this)
        setContentView(R.layout.activity_external_upload)
        folderSelect.text = path.toString()
        if (presenter.availableIdentities.isEmpty()) {
            finish()
            return
        }
        identity = presenter.availableIdentities.first()
    }

    override fun startUpload(documentId: DocumentId) {
    }
}
