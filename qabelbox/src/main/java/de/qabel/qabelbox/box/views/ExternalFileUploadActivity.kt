package de.qabel.qabelbox.box.views

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import de.qabel.box.storage.dto.BoxPath
import de.qabel.core.logging.QabelLog
import de.qabel.qabelbox.R
import de.qabel.qabelbox.base.ACTIVE_IDENTITY
import de.qabel.qabelbox.base.CrashReportingActivity
import de.qabel.qabelbox.box.interactor.BoxServiceStarter
import de.qabel.qabelbox.box.presenters.FileUploadPresenter
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.provider.DocumentIdParser
import de.qabel.qabelbox.dagger.modules.ActivityModule
import de.qabel.qabelbox.dagger.modules.ExternalFileUploadModule
import kotlinx.android.synthetic.main.activity_external_upload.*
import org.jetbrains.anko.onClick
import org.jetbrains.anko.startActivityForResult
import java.io.FileNotFoundException
import javax.inject.Inject
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class ExternalFileUploadActivity() : FileUploadView, CrashReportingActivity(), QabelLog {

    override lateinit var identity: FileUploadPresenter.IdentitySelection
    override var path: BoxPath.FolderLike = BoxPath.Root / "Upload"
    override var filename: String by Delegates.observable("") {
        kProperty: KProperty<*>, old: String, new: String ->
        if (old != new) {
            filenameField.setText(new, TextView.BufferType.EDITABLE)
        }
    }


    @Inject
    lateinit var documentIdParser: DocumentIdParser

    @Inject
    lateinit var presenter: FileUploadPresenter

    @Inject
    lateinit var boxServiceStarter: BoxServiceStarter

    var fileUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FOLDER_CHOOSER_RESULT && resultCode == Activity.RESULT_OK) {
            data?.let { data ->
                val docId = data.extras.getString(FolderChooserActivity.FOLDER_DOCUMENT_ID)
                val id = try {
                    documentIdParser.parse(docId)
                } catch (e: FileNotFoundException) {
                    return
                }
                if (id.path is BoxPath.FolderLike) {
                    path = id.path
                    folderSelect.text = id.path.toString()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.ab_external_upload, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.confirmUpload) {
            presenter.confirm()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applicationComponent.plus(ActivityModule(this))
                .plus(ExternalFileUploadModule((this)))
                .inject(this)
        setContentView(R.layout.activity_external_upload)
        setSupportActionBar(toolbar)
        folderSelect.text = path.toString()
        if (presenter.availableIdentities.isEmpty()) {
            finish()
            return
        }
        populateSpinner()
        folderSelect.onClick {
            startActivityForResult<FolderChooserActivity>(FOLDER_CHOOSER_RESULT,
                    ACTIVE_IDENTITY to identity.keyId)
        }
        if (Intent.ACTION_SEND == intent.action) {
            fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            filename = fileUri?.lastPathSegment ?: "filename"
        }
        if (fileUri == null) {
            finish()
        }
    }

    private fun populateSpinner() {
        val identities = presenter.availableIdentities
        val adapter = ArrayAdapter<FileUploadPresenter.IdentitySelection>(
                this, R.layout.identity_spinner_field, identities)
        identitySelect.adapter = adapter
        identitySelect.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) { }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                identity = adapter.getItem(position)
            }

        }
    }

    override fun startUpload(documentId: DocumentId) {
        fileUri?.let {
            boxServiceStarter.startUpload(documentId, it)
            finish()
        }
    }

    companion object {
        const val FOLDER_CHOOSER_RESULT = 1
    }
}

