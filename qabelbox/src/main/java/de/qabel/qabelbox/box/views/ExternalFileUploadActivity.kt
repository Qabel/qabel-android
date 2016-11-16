package de.qabel.qabelbox.box.views

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import de.qabel.qabelbox.box.queryNameAndSize
import de.qabel.qabelbox.contacts.extensions.colorForKeyIdentitfier
import de.qabel.qabelbox.contacts.view.widgets.IdentityIconDrawable
import de.qabel.qabelbox.dagger.modules.ActivityModule
import de.qabel.qabelbox.dagger.modules.ExternalFileUploadModule
import de.qabel.qabelbox.identity.view.adapter.IdentitiesAdapter
import kotlinx.android.synthetic.main.activity_external_upload.*
import kotlinx.android.synthetic.main.item_identities.view.*
import org.jetbrains.anko.ctx
import org.jetbrains.anko.layoutInflater
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
                    ACTIVE_IDENTITY to identity.keyId,
                    FolderChooserActivity.TEST_RUN to intent.getBooleanExtra(TEST_RUN, false))
        }
        if (Intent.ACTION_SEND == intent.action) {
            fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        fileUri.let {
            if (it == null) {
                finish()
            } else {
                filename = try {
                    contentResolver.queryNameAndSize(it).first
                } catch (e: NullPointerException) {
                    fileUri?.lastPathSegment ?: "filename"
                }
            }

        }
    }
    private val splitRegex: Regex by lazy { " ".toRegex() }
    private fun String.toInitials(): String = split(splitRegex).filter {
        it.isNotEmpty() && it.first().isLetterOrDigit()
    }.take(2).map {
        it.take(1).toUpperCase()
    }.joinToString("")

    private fun populateSpinner() {
        val identities = presenter.availableIdentities
        val iconSize = ctx.resources.
                getDimension(R.dimen.material_drawer_item_profile_icon_width).toInt()

        val adapter = object: ArrayAdapter<FileUploadPresenter.IdentitySelection>(
                this, R.layout.item_identities,R.id.item_name , identities) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
                return getView(position, convertView, parent)
            }
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = convertView ?: ctx.layoutInflater.inflate(R.layout.item_identities, parent, false)
                val item = getItem(position)
                view.item_name.text = item.alias
                view.item_icon.background = IdentityIconDrawable(
                        width = iconSize,
                        height = iconSize,
                        text = item.alias.toInitials(),
                        color = colorForKeyIdentitfier(item.keyId, 0, ctx))
                return view
            }
        }
        identitySelect.adapter = adapter
        identitySelect.setSelection(0)
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
        const val TEST_RUN = "TEST_RUN"
    }
}

