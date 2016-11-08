package de.qabel.qabelbox.box.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.support.v7.widget.LinearLayoutManager
import android.text.InputType
import android.view.*
import com.cocosw.bottomsheet.BottomSheet
import de.qabel.box.storage.exceptions.QblStorageException
import de.qabel.core.config.Identity
import de.qabel.core.repository.ContactRepository
import de.qabel.core.ui.displayName
import de.qabel.qabelbox.BuildConfig
import de.qabel.qabelbox.R
import de.qabel.qabelbox.base.BaseFragment
import de.qabel.qabelbox.box.adapters.FileAdapter
import de.qabel.box.storage.dto.BoxPath
import de.qabel.core.event.EventDispatcher
import de.qabel.qabelbox.box.dto.BrowserEntry
import de.qabel.qabelbox.box.dto.FileOperationState
import de.qabel.qabelbox.box.dto.FileOperationState.Status
import de.qabel.qabelbox.box.events.FileUploadEvent
import de.qabel.qabelbox.box.presenters.FileBrowserPresenter
import de.qabel.qabelbox.box.provider.BoxProvider
import de.qabel.qabelbox.box.provider.DocumentId
import de.qabel.qabelbox.box.provider.toDocumentId
import de.qabel.qabelbox.box.queryNameAndSize
import de.qabel.qabelbox.dagger.components.ActiveIdentityComponent
import de.qabel.qabelbox.dagger.modules.FileBrowserModule
import de.qabel.qabelbox.dagger.modules.FileBrowserViewModule
import de.qabel.qabelbox.ui.extensions.showEnterTextDialog
import de.qabel.qabelbox.viewer.ImageViewerActivity
import kotlinx.android.synthetic.main.fragment_files.*
import org.jetbrains.anko.*
import rx.Subscription
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URLConnection
import java.util.*
import javax.inject.Inject

class FileBrowserFragment : FileBrowserView, FileListingView,
        BaseFragment(mainFragment = true, showOptionsMenu = true, showFAButton = true), AnkoLogger {

    companion object {
        fun newInstance() = FileBrowserFragment()
        val REQUEST_OPEN_FILE = 0
        val REQUEST_EXPORT_FILE = 1
        val KEY_EXPORT_DOCUMENT_ID = "EXPORT_DOCUMENT_ID"
    }

    var exportDocumentId: DocumentId? = null

    override val title: String by lazy { ctx.getString(R.string.filebrowser) }

    override val subtitle: String?
        get() = if (presenter.path !is BoxPath.Root) presenter.path.toString() else null

    @Inject
    lateinit var presenter: FileBrowserPresenter

    @Inject
    lateinit var eventDispatcher: EventDispatcher
    lateinit var subscription: Subscription

    @Inject
    lateinit var contactRepository: ContactRepository

    @Inject
    lateinit var identity: Identity

    lateinit var adapter: FileAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val component = getComponent(ActiveIdentityComponent::class.java)
                .plus(FileBrowserViewModule(this))
        component.inject(this)
        savedInstanceState?.let {
            savedInstanceState.getString(KEY_EXPORT_DOCUMENT_ID, null)?.let {
                try {
                    exportDocumentId = it.toDocumentId()
                } catch (ignored: QblStorageException) {
                }
            }
        }

        adapter = FileAdapter(mutableListOf(),
                click = { presenter.onClick(it) }, longClick = { openBottomSheet(it) }
        )

        files_list.layoutManager = LinearLayoutManager(ctx)
        files_list.adapter = adapter
        swipeRefresh.isEnabled = false
    }

    override fun onResume() {
        super.onResume()
        if (!(mActivity?.TEST ?: false)) {
            presenter.onRefresh()
        }
        subscription = eventDispatcher.events(FileUploadEvent::class.java).subscribe {
            if (listOf(Status.COMPLETE, Status.ERROR).contains(it.operation.status)) {
                presenter.onRefresh()
                backgroundRefreshDone()
            } else {
                backgroundRefreshStart()
            }
        }
    }

    override fun onPause() {
        bottomSheet?.dismiss()
        subscription.unsubscribe()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        if (exportDocumentId != null) {
            outState?.putString(KEY_EXPORT_DOCUMENT_ID, exportDocumentId.toString())
        }
        super.onSaveInstanceState(outState)
    }


    override fun showError(throwable: Throwable) {
        runOnUiThread {
            longToast(throwable.message ?: "Error")
            error("Error", throwable)
            refreshDone()
        }
    }

    private var bottomSheet: BottomSheet? = null

    fun openBottomSheet(entry: BrowserEntry) {
        val (icon, sheet) = when (entry) {
            is BrowserEntry.File -> Pair(R.drawable.file, R.menu.bottom_sheet_files)
            is BrowserEntry.Folder -> Pair(R.drawable.folder, R.menu.bottom_sheet_folder)
        }
        bottomSheet?.dismiss()
        bottomSheet = BottomSheet.Builder(activity).title(entry.name).icon(icon).sheet(sheet).listener {
            dialogInterface, menu_id ->
            when (entry) {
                is BrowserEntry.File -> when (menu_id) {
                    R.id.share -> presenter.share(entry)
                    R.id.unshare -> presenter.unShareFile(entry)
                    R.id.delete -> presenter.delete(entry)
                    R.id.export -> presenter.export(entry)
                    R.id.forward -> chooseContact(entry)
                }
                is BrowserEntry.Folder -> when (menu_id) {
                    R.id.delete -> presenter.deleteFolder(entry)
                }
            }
        }.apply {
            if (entry is BrowserEntry.File) {
                if (!entry.sharedTo.isNotEmpty()) {
                    this.remove(R.id.unshare)
                }
            }
        }.show()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_files, container, false)
                ?: throw IllegalStateException("Could not create view")
    }

    override fun showEntries(entries: List<BrowserEntry>) {
        runOnUiThread {
            adapter.entries.clear()
            if (entries.size > 0) {
                empty_view.visibility = View.INVISIBLE
            } else {
                empty_view.visibility = View.VISIBLE
            }
            adapter.entries.addAll(entries)
            adapter.notifyDataSetChanged()
            refreshToolbarTitle()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val uri = data?.data
        if (uri != null) {
            when (requestCode) {
                REQUEST_OPEN_FILE -> {
                    upload(uri)
                    return
                }
                REQUEST_EXPORT_FILE -> {
                    exportFile(uri)
                    return
                }
            }

        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun exportFile(uri: Uri?): Boolean {
        val exportId = exportDocumentId ?: return true
        doAsync() {
            val input = ctx.contentResolver.openInputStream(uriFromDocumentId(exportId))
            val output = ctx.contentResolver.openOutputStream(uri)
            if (input == null || output == null) {
                toast(R.string.export_aborted)
                return@doAsync
            }
            try {
                input.copyTo(output)
            } catch (e: IOException) {
                toast(R.string.export_aborted)
                return@doAsync
            }
            runOnUiThread {
                toast(R.string.export_complete)
            }
        }
        return false
    }

    private fun upload(fileUri: Uri) {
        try {
            with(ctx.contentResolver) {
                val (filename, size) = queryNameAndSize(fileUri)
                toast("Uploading $filename with size $size")
                presenter.upload(ctx, BrowserEntry.File(filename, size, Date()), fileUri)
            }
        } catch (e: FileNotFoundException) {
            toast(R.string.upload_failed_title)
            return
        }
    }

    override fun refreshStart() {
        runOnUiThread {
            if (!swipeRefresh.isRefreshing) {
                swipeRefresh.isRefreshing = true
            }
        }
    }

    override fun refreshDone() {
        runOnUiThread {
            swipeRefresh.isRefreshing = false
        }
    }

    fun backgroundRefreshStart() {
        runOnUiThread {
            background_progress_bar?.visibility = View.VISIBLE
        }
    }

    fun backgroundRefreshDone() {
        runOnUiThread {
            background_progress_bar?.visibility = View.INVISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu?.clear()
        inflater?.inflate(R.menu.ab_files, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_refresh -> presenter.onRefresh()
            R.id.menu_up -> presenter.navigateUp()
            else -> return false
        }
        return true
    }

    override fun open(documentId: DocumentId) {
        runOnUiThread {
            info("Open With via started for docu id $documentId")
            val uri = uriFromDocumentId(documentId)
            val mimeType = typeFromUri(uri)
            if (mimeType.startsWith("image")) {
                val intent = Intent(ctx, ImageViewerActivity::class.java)
                intent.putExtra(ImageViewerActivity.P_URI, uri)
                intent.putExtra(ImageViewerActivity.P_TYPE, mimeType)
                startActivity(intent)
            } else {
                startViewIntent(mimeType, uri)
            }
        }
    }

    private fun startViewIntent(mimeType: String, uri: Uri?) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = uri
            type = mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, ctx.getString(R.string.chooser_open_with)).singleTop())
    }

    override fun share(documentId: DocumentId) {
        runOnUiThread {
            info("Share via started for docu id $documentId")
            val uri = uriFromDocumentId(documentId)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                data = uri
                type = typeFromUri(uri)
                putExtra(Intent.EXTRA_SUBJECT, R.string.share_subject)
                putExtra(Intent.EXTRA_TITLE, R.string.share_subject)
                putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.share_text))
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, activity.getString(R.string.share_via)))
        }
    }

    private fun typeFromUri(uri: Uri?): String {
        return URLConnection.guessContentTypeFromName(uri.toString()) ?: "application/octet-stream"
    }

    private fun uriFromDocumentId(documentId: DocumentId): Uri? =
            DocumentsContract.buildDocumentUri(
                    BuildConfig.APPLICATION_ID + BoxProvider.AUTHORITY,
                    documentId.toString())


    override fun export(documentId: DocumentId) {
        runOnUiThread {
            exportDocumentId = documentId

            val uri = uriFromDocumentId(documentId)
            val mimeType = typeFromUri(uri)
            val createDocument = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                data = uri
                type = mimeType
                putExtra(Intent.EXTRA_TITLE, documentId.path.name)
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            info(createDocument.toString())
            startActivityForResult(createDocument, REQUEST_EXPORT_FILE)
        }
    }

    override fun handleFABAction(): Boolean {
        bottomSheet?.dismiss()
        bottomSheet = BottomSheet.Builder(activity).sheet(R.menu.bottom_sheet_files_add).listener { dialog, which ->
            when (which) {
                R.id.upload_file -> {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                    startActivityForResult(intent, REQUEST_OPEN_FILE)
                }
                R.id.create_folder -> {
                    createFolderDialog()
                }
            }
        }.show()
        return true
    }

    private fun chooseContact(entry: BrowserEntry.File) {
        UI {
            val contacts = contactRepository.find(identity).contacts.map {
                Pair(it, it.displayName())
            }
            selector(ctx.getString(R.string.contact), contacts.map { it.second }) { i ->
                val contact = contacts[i].first
                presenter.shareToContact(entry, contact)
            }
        }
    }

    private fun createFolderDialog() =
            showEnterTextDialog(R.string.create_folder,
                    R.string.add_folder_name, InputType.TYPE_CLASS_TEXT, {
                presenter.createFolder(BrowserEntry.Folder(it))
            })

    override fun onBackPressed(): Boolean {
        return presenter.navigateUp()
    }

}

