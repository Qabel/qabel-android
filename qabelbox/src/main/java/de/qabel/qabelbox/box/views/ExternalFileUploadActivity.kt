package de.qabel.qabelbox.box.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import de.qabel.box.storage.dto.BoxPath
import de.qabel.core.logging.QabelLog
import de.qabel.qabelbox.R
import de.qabel.qabelbox.base.ACTIVE_IDENTITY
import de.qabel.qabelbox.base.CrashReportingActivity
import de.qabel.qabelbox.box.presenters.FileUploadPresenter
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.provider.DocumentIdParser
import de.qabel.qabelbox.dagger.modules.ActivityModule
import de.qabel.qabelbox.dagger.modules.ExternalFileUploadModule
import kotlinx.android.synthetic.main.activity_external_upload.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.appBarLayout
import java.io.FileNotFoundException
import javax.inject.Inject

class ExternalFileUploadActivity() : FileUploadView, CrashReportingActivity(), QabelLog {

    override lateinit var identity: FileUploadPresenter.IdentitySelection
    override var path: BoxPath = BoxPath.Root / "Upload"
    override var filename: String = ""

    @Inject
    lateinit var documentIdParser: DocumentIdParser

    @Inject
    lateinit var presenter: FileUploadPresenter

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FOLDER_CHOOSER_RESULT && resultCode == Activity.RESULT_OK) {
            data?.let { data ->
                val docId = data.extras.getString(FolderChooserActivity.FOLDER_DOCUMENT_ID)
                val id = try {
                    documentIdParser.parse(docId)
                } catch (e: FileNotFoundException) {
                    return
                }
                path = id.path
                folderSelect.text = id.path.toString()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationComponent.plus(ActivityModule(this))
                .plus(ExternalFileUploadModule((this)))
                .inject(this)
        ExternalFileUploadUi().setContentView(this)
        folderSelect.text = path.toString()
        if (presenter.availableIdentities.isEmpty()) {
            finish()
            return
        }
        folderSelect.onClick {
            startActivityForResult<FolderChooserActivity>(FOLDER_CHOOSER_RESULT,
                    ACTIVE_IDENTITY to identity.keyId)
        }
        identity = presenter.availableIdentities.sortedBy { it.alias }.first()
    }

    override fun startUpload(documentId: DocumentId) {
    }

    companion object {
        const val FOLDER_CHOOSER_RESULT = 1
    }
}

class ExternalFileUploadUi: AnkoComponent<ExternalFileUploadActivity> {
    override fun createView(ui: AnkoContext<ExternalFileUploadActivity>): View =
        with (ui) {
                verticalLayout {
                    appBarLayout {
                        toolbar { }
                    }
                    spinner {
                        id = R.id.identitySelect
                    }
                    editText {
                        id = R.id.filenameField
                    }
                    button {
                        id = R.id.folderSelect
                    }
                }
        }
}
