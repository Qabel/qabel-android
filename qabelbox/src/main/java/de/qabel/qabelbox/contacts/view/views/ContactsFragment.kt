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
import butterknife.BindView
import butterknife.ButterKnife
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
import de.qabel.qabelbox.navigation.Navigator
import de.qabel.qabelbox.ui.extensions.showConfirmation
import de.qabel.qabelbox.ui.extensions.showMessage
import de.qabel.qabelbox.ui.extensions.showQuantityMessage
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.android.synthetic.main.fragment_contacts.view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.ctx
import org.jetbrains.anko.debug
import org.jetbrains.anko.onUiThread
import java.io.File
import javax.inject.Inject

class ContactsFragment() : ContactsView, BaseFragment(), AnkoLogger, SearchView.OnQueryTextListener {

    override var searchString: String? = null

    var injectCompleted = false

    @Inject
    lateinit var presenter: ContactsPresenter
    @Inject
    lateinit var navigator: Navigator
    @Inject
    lateinit var identity: Identity

    @BindView(R.id.contact_search)
    lateinit var contactSearch: SearchView

    val adapter = ContactsAdapter({ contact ->
        if (contact.active) navigator.selectChatFragment(contact.contact.keyIdentifier)
        else navigator.selectContactDetailsFragment(contact);
    }, { contact ->
        BottomSheet.Builder(activity).title(contact.contact.alias).sheet(R.menu.bottom_sheet_contactlist).
                listener({ dialog, which ->
                    when (which) {
                        R.id.contact_list_item_details -> navigator.selectContactDetailsFragment(contact)
                        R.id.contact_list_item_delete -> showDeleteContactConfirmation(contact)
                        R.id.contact_list_item_export -> presenter.startContactExport(contact)
                        R.id.contact_list_item_qrcode -> navigator.selectQrCodeFragment(contact.contact)
                        R.id.contact_list_item_send -> presenter.sendContact(contact, activity.externalCacheDir)
                    }
                }).show();
        true;
    });

    override fun startShareDialog(targetFile: File) {
        ExternalApps.share(activity, Uri.fromFile(targetFile), "application/json");
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val component = getComponent(MainActivityComponent::class.java).plus(ContactsModule(this))
        component.inject(this);
        injectCompleted = true
        presenter.refresh();

        setHasOptionsMenu(true);
        configureAsMainFragment();

        contact_list.layoutManager = LinearLayoutManager(view.context);
        contact_list.adapter = adapter;
        updateView(adapter.getContactCount());
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ButterKnife.bind(this, view as  View);
        contactSearch.setOnQueryTextListener(this)
        contactSearch.queryHint = getString(R.string.search);
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        UIHelper.hideKeyboard(activity, view);
        return false;
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        searchString = newText;
        presenter.refresh();
        return true;
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

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.ab_contacts, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_contact_export_all -> presenter.startContactsExport()
        }
        return true;
    }

    override fun showEmpty() {
        loadData(listOf());
    }

    private fun updateView(itemCount: Int) {
        if (itemCount == 0) {
            contact_list?.empty_view?.visibility = View.VISIBLE
            contactCount?.visibility = View.GONE;
        } else {
            contact_list?.empty_view?.visibility = View.GONE
            contactCount?.visibility = View.VISIBLE;
            contactCount?.text = getString(R.string.contact_count, itemCount);
        }
    }

    override fun loadData(data: List<ContactDto>) {
        debug("Filling adapter with ${data.size} contacts")
        busy()
        onUiThread {
            adapter.refresh(data)
            adapter.notifyDataSetChanged()
            updateView(data.size);
            idle();
        }
    }

    override fun getTitle(): String = ctx.getString(R.string.Contacts)

    override fun isFabNeeded(): Boolean {
        return true;
    }

    override fun handleFABAction(): Boolean {
        BottomSheet.Builder(activity).title(R.string.add_new_contact).sheet(R.menu.bottom_sheet_add_contact).listener { dialog, which ->
            when (which) {
                R.id.add_contact_from_file -> presenter.startContactsImport()
                R.id.add_contact_via_qr -> presenter.startContactImportScan(IntentIntegrator.REQUEST_CODE)
            }
        }.show()
        return true;
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
        startActivityForResult(intent, requestCode);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  resultData: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val action = presenter.externalAction;
            if (action == null || action.requestCode != requestCode) {
                debug("Dismatching  activity result. Cancel handling.");
                return;
            }
            when (action) {
                is ExternalFileAction -> {
                    val uri = resultData?.data
                    debug("Open file " + uri.toString() + " WITH MODE " + action.accessMode);
                    val file = activity.contentResolver.openFileDescriptor(uri, action.accessMode);
                    presenter.handleExternalFileAction(action, file.fileDescriptor);
                }
                else -> {
                    val scanResult = IntentIntegrator.parseActivityResult(requestCode, Activity.RESULT_OK, resultData)
                    presenter.handleScanResult(action, scanResult.contents);
                }
            }
        } else {
            //TODO Display action canceled
            presenter.externalAction = null;
        }
    }

    override fun showImportFailed() {
        showMessage(R.string.dialog_headline_warning, R.string.contact_import_failed);
    }

    override fun showImportSuccess(imported: Int, size: Int) {
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

    override fun showExportFailed() {
        showMessage(R.string.dialog_headline_warning,
                R.string.contact_export_failed);
    }

    override fun showExportSuccess(size: Int) {
        showQuantityMessage(R.string.dialog_headline_info,
                R.plurals.contact_export_successfully, size, size)
    }

    fun showDeleteContactConfirmation(contact: ContactDto) {
        showConfirmation(R.string.confirm_delete_title, R.string.dialog_message_delete_contact_question,
                contact.contact.alias, {
            presenter.deleteContact(contact)
        });
    }

    override fun showContactDeletedMessage(contact : ContactDto){
        showMessage(R.string.dialog_headline_info,
                R.string.contact_deleted,
                contact.contact.alias);
    }

}
