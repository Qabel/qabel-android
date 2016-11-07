package de.qabel.qabelbox.contacts.view.views

import android.os.Bundle
import android.view.*
import butterknife.ButterKnife
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.dagger.ContactDetailsModule
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.view.adapters.ContactDetailsAdapter
import de.qabel.qabelbox.contacts.view.presenters.ContactDetailsPresenter
import de.qabel.qabelbox.dagger.components.ActiveIdentityComponent
import de.qabel.qabelbox.base.BaseFragment
import org.jetbrains.anko.AnkoLogger
import javax.inject.Inject


class ContactDetailsFragment() : ContactDetailsView, BaseFragment(showOptionsMenu = true), AnkoLogger {

    companion object {
        val ARG_CONTACT = "CONTACT"

        fun withContactKey(contactKey: String): ContactDetailsFragment {
            val fragment = ContactDetailsFragment()
            fragment.arguments = with(Bundle()) {
                putString(ARG_CONTACT, contactKey)
                this
            }
            return fragment
        }
    }

    val adapter = ContactDetailsAdapter({ presenter.onSendMsgClick(it) })
    lateinit override var contactKeyId: String
    @Inject lateinit var presenter: ContactDetailsPresenter

    var injectCompleted = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contactKeyId = arguments.getString(ARG_CONTACT) ?: throw IllegalArgumentException(
                "Starting ContactDetailsFragment without contactKeyId")

        val component = getComponent(ActiveIdentityComponent::class.java).plus(ContactDetailsModule(this))
        component.inject(this)
        injectCompleted = true
    }

    override fun onResume() {
        super.onResume()
        presenter.refreshContact()
    }


    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ButterKnife.bind(this, view as View)
        adapter.view = view
    }

    override fun onDestroyView() {
        adapter.view = null
        super.onDestroyView()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_contact_details, container, false)
                ?: throw IllegalStateException("Could not create view")
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.ab_contact_details, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_contact_edit -> presenter.handleEditClick()
        }
        return true
    }

    override val title: String
        get() = (if (injectCompleted) presenter.title else "")

    override fun loadContact(contact: ContactDto) = adapter.loadContact(contact)

}

