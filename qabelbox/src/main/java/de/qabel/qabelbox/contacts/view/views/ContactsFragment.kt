package de.qabel.qabelbox.contacts.view.views

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.*
import com.cocosw.bottomsheet.BottomSheet
import com.google.zxing.integration.android.IntentIntegrator
import de.qabel.core.config.Identity
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.dagger.ContactsModule
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.view.adapters.ContactsAdapter
import de.qabel.qabelbox.contacts.view.presenters.ContactsPresenter
import de.qabel.qabelbox.dagger.components.MainActivityComponent
import de.qabel.qabelbox.external.ExternalFileAction
import de.qabel.qabelbox.fragments.BaseFragment
import de.qabel.qabelbox.helper.ExternalApps
import de.qabel.qabelbox.helper.UIHelper
import de.qabel.qabelbox.index.AndroidIndexSyncService
import de.qabel.qabelbox.navigation.Navigator
import de.qabel.qabelbox.ui.extensions.showConfirmation
import de.qabel.qabelbox.ui.extensions.showMessage
import de.qabel.qabelbox.ui.extensions.showQuantityMessage
import kotlinx.android.synthetic.main.fragment_contacts.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.ctx
import org.jetbrains.anko.debug
import org.jetbrains.anko.onUiThread
import java.io.File
import javax.inject.Inject

class ContactsFragment() : ContactsView, BaseFragment(), AnkoLogger, SearchView.OnQueryTextListener {

    override var searchString: String? = null
    override var showIgnored: Boolean = false

    var injectCompleted = false

    @Inject
    lateinit var presenter: ContactsPresenter
    @Inject
    lateinit var navigator: Navigator
    @Inject
    lateinit var identity: Identity

    val adapter = ContactsAdapter({
        contact -> presenter.handleClick(contact) }, {
        contact -> presenter.handleLongClick(contact)
    })

    override fun showBottomSheet(contact: ContactDto) {
        BottomSheet.Builder(activity).title(contact.contact.alias).sheet(R.menu.bottom_sheet_contactlist).
                listener({ dialog, which ->
                    when (which) {
                        R.id.contact_list_item_details -> navigator.selectContactDetailsFragment(contact)
                        R.id.contact_list_item_edit -> navigator.selectContactEdit(contact)
                        R.id.contact_list_item_delete -> showDeleteContactConfirmation(contact)
                        R.id.contact_list_item_export -> presenter.startContactExport(contact)
                        R.id.contact_list_item_qrcode -> navigator.selectQrCodeFragment(contact.contact)
                        R.id.contact_list_item_send -> presenter.sendContact(contact, activity.externalCacheDir)
                    }
                }).show()
    }

    override fun startShareDialog(targetFile: File) {
        ExternalApps.share(activity, Uri.fromFile(targetFile), "application/json")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val component = getComponent(MainActivityComponent::class.java).plus(ContactsModule(this))
        component.inject(this)
        injectCompleted = true

        setHasOptionsMenu(true)
        configureAsMainFragment()

        contact_list.layoutManager = LinearLayoutManager(view.context)
        contact_list.adapter = adapter
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contact_search.setOnQueryTextListener(this)
        contact_search.queryHint = getString(R.string.search)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        UIHelper.hideKeyboard(activity, view)
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        searchString = newText
        presenter.refresh()
        return true
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (injectCompleted) {
                presenter.refresh()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateView(adapter.getContactCount())
        presenter.refresh()
        ctx.registerReceiver(broadcastReceiver, IntentFilter(QblBroadcastConstants.Contacts.CONTACTS_CHANGED))
    }

    override fun onPause() {
        super.onPause()
        ctx.unregisterReceiver(broadcastReceiver)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_contacts, container, false)
                ?: throw IllegalStateException("Could not create view")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.ab_contacts, menu)
        menu.findItem(R.id.action_show_ignored).isChecked = showIgnored
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_contact_export_all -> presenter.startContactsExport()
            R.id.action_show_ignored -> {
                item.isChecked = !item.isChecked
                showIgnored = item.isChecked
                presenter.refresh()
            }
            R.id.refresh -> AndroidIndexSyncService.startSyncContacts(ctx)
        }
        return true
    }

    override fun showEmpty() {
        loadData(listOf())
    }

    private fun updateView(itemCount: Int) {
        if (itemCount == 0) {
            empty_view?.visibility = View.VISIBLE
            contactCount?.visibility = View.GONE
        } else {
            empty_view?.visibility = View.GONE
            contactCount?.visibility = View.VISIBLE
            contactCount?.text = getString(R.string.contact_count, itemCount)
        }
    }

    override fun loadData(data: List<ContactDto>) {
        debug("Filling adapter with ${data.size} contacts")
        busy()
        onUiThread {
            adapter.refresh(data)
            adapter.notifyDataSetChanged()
            updateView(data.size)
            idle()
        }
    }

    override val title: String by lazy { ctx.getString(R.string.Contacts) }
    override val isFabNeeded = true

    override fun handleFABAction(): Boolean {
        BottomSheet.Builder(activity).title(R.string.add_new_contact).sheet(R.menu.bottom_sheet_add_contact).listener { dialog, which ->
            when (which) {
                R.id.add_contact_from_file -> presenter.startContactsImport()
                R.id.add_contact_via_qr -> presenter.startContactImportScan(IntentIntegrator.REQUEST_CODE)
            }
        }.show()
        return true
    }

    override fun startQRScan() {
        val integrator = IntentIntegrator(this)
        integrator.initiateScan()
    }

    override fun startExportFileChooser(filename: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/json"
        intent.putExtra(Intent.EXTRA_TITLE, filename)
        startActivityForResult(intent, requestCode)
    }

    override fun startImportFileChooser(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  resultData: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val action = presenter.externalAction
            if (action == null || action.requestCode != requestCode) {
                debug("Dismatching  activity result. Cancel handling.")
                return
            }
            when (action) {
                is ExternalFileAction -> {
                    val uri = resultData?.data
                    debug("Open file " + uri.toString() + " WITH MODE " + action.accessMode)
                    val file = activity.contentResolver.openFileDescriptor(uri, action.accessMode)
                    presenter.handleExternalFileAction(action, file.fileDescriptor)
                }
                else -> {
                    val scanResult = IntentIntegrator.parseActivityResult(requestCode, Activity.RESULT_OK, resultData)
                    presenter.handleScanResult(action, scanResult.contents)
                }
            }
        } else {
            debug("External Action failed!")
            presenter.externalAction = null
        }
    }

    override fun showImportFailedMessage() {
        showMessage(R.string.dialog_headline_warning, R.string.contact_import_failed)
    }

    override fun showImportSuccessMessage(imported: Int, size: Int) {
        if (imported > 0) {
            if (imported == 1 && imported == size) {
                showMessage(R.string.dialog_headline_info,
                        R.string.contact_import_successfull)
            } else {
                showMessage(R.string.dialog_headline_info,
                        R.string.contact_import_successfull_many,
                        imported,
                        size)
            }
        } else {
            showMessage(R.string.dialog_headline_info,
                    R.string.contact_import_zero_additions)
        }
    }

    override fun showExportFailedMessage() {
        showMessage(R.string.dialog_headline_warning,
                R.string.contact_export_failed)
    }

    override fun showExportSuccessMessage(size: Int) {
        showQuantityMessage(R.string.dialog_headline_info,
                R.plurals.contact_export_successfully, size, size)
    }

    fun showDeleteContactConfirmation(contact: ContactDto) {
        showConfirmation(R.string.confirm_delete_title, R.string.confirm_delete_message,
                contact.contact.alias, {
            presenter.deleteContact(contact)
        })
    }

    override fun showContactDeletedMessage(contact: ContactDto) {
        showMessage(R.string.dialog_headline_info,
                R.string.entry_deleted,
                contact.contact.alias)
    }

    override fun showContactExistsMessage() {
        showMessage(R.string.dialog_headline_info, R.string.contact_exists)
    }

}
