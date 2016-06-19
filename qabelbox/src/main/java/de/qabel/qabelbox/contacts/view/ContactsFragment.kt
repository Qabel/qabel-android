package de.qabel.qabelbox.contacts.view

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cocosw.bottomsheet.BottomSheet
import com.google.zxing.integration.android.IntentIntegrator
import de.qabel.core.config.Identity
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.ContactsRequestCodes
import de.qabel.qabelbox.contacts.dagger.ContactsModule
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.navigation.ContactsNavigator
import de.qabel.qabelbox.contacts.view.adapters.ContactsAdapter
import de.qabel.qabelbox.contacts.view.presenters.ContactsPresenter
import de.qabel.qabelbox.dagger.components.MainActivityComponent
import de.qabel.qabelbox.fragments.BaseFragment
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.android.synthetic.main.fragment_contacts.view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.ctx
import org.jetbrains.anko.debug
import org.jetbrains.anko.onUiThread
import javax.inject.Inject

class ContactsFragment : ContactsView, BaseFragment(), AnkoLogger {

    var injectCompleted = false

    val adapter = ContactsAdapter({
        //TODO Contact Clicked
    }, { contact ->
        BottomSheet.Builder(activity).title(contact.contact.alias).sheet(R.menu.bottom_sheet_contactlist).
                listener({ dialog, which ->
                    when (which) {
                        R.id.contact_list_item_delete -> presenter.deleteContact(activity, contact)
                        R.id.contact_list_item_export -> presenter.exportContact(contact)
                        R.id.contact_list_item_qrcode -> navigator.showQrCodeFragment(activity, contact.contact)
                        R.id.contact_list_item_send -> presenter.sendContact(activity, contact)
                    }
                }).show();
        true;
    });

    @Inject
    lateinit var presenter: ContactsPresenter
    @Inject
    lateinit var navigator: ContactsNavigator
    @Inject
    lateinit var identity: Identity

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val component = getComponent(MainActivityComponent::class.java).plus(ContactsModule(this))
        component.inject(this);
        injectCompleted = true

        contact_list.layoutManager = LinearLayoutManager(view.context);
        contact_list.adapter = adapter;
        updateView(adapter.getContactCount());
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (injectCompleted) {
                presenter.refresh()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ctx.registerReceiver(broadcastReceiver, IntentFilter(QblBroadcastConstants.Contacts.CONTACTS_CHANGED))
    }

    override fun onStop() {
        super.onStop()
        ctx.unregisterReceiver(broadcastReceiver)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_contacts, container, false)
                ?: throw IllegalStateException("Could not create view")
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
                R.id.add_contact_from_file -> {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "*/*"
                    startActivityForResult(intent, ContactsRequestCodes.REQUEST_IMPORT_CONTACT);
                }
                R.id.add_contact_via_qr -> {
                    val integrator = IntentIntegrator(this)
                    integrator.initiateScan()
                }
            }
        }.show()
        return true;
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  resultData: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            presenter.handleActivityResult(activity, requestCode, resultData)
        }
    }
}