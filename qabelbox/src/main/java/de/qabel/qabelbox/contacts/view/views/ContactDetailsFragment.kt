package de.qabel.qabelbox.contacts.view.views

import android.os.Bundle
import android.view.*
import butterknife.ButterKnife
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.dagger.ContactDetailsModule
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.view.adapters.ContactDetailsAdapter
import de.qabel.qabelbox.contacts.view.presenters.ContactDetailsPresenter
import de.qabel.qabelbox.dagger.components.MainActivityComponent
import de.qabel.qabelbox.fragments.BaseFragment
import de.qabel.qabelbox.navigation.Navigator
import de.qabel.qabelbox.ui.views.ChatFragment
import org.jetbrains.anko.AnkoLogger
import javax.inject.Inject


class ContactDetailsFragment() : ContactDetailsView, BaseFragment(), AnkoLogger {

    companion object {
        val ARG_CONTACT = "CONTACT"

        fun withContact(contact: ContactDto): ContactDetailsFragment {
            val fragment = ContactDetailsFragment()
            fragment.arguments = with(Bundle()) {
                putString(ARG_CONTACT, contact.contact.keyIdentifier)
                this
            }
            return fragment
        }
    }

    var adapter = ContactDetailsAdapter({ identity -> navigator.selectContactChat(contactKeyId, identity) })
    lateinit override var contactKeyId: String
    @Inject lateinit var presenter: ContactDetailsPresenter
    @Inject lateinit var navigator: Navigator

    var injectCompleted = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contactKeyId = arguments.getString(ChatFragment.ARG_CONTACT) ?: throw IllegalArgumentException(
                "Starting ContactDetailsFragment without contactKeyId")

        val component = getComponent(MainActivityComponent::class.java).plus(ContactDetailsModule(this))
        component.inject(this);
        injectCompleted = true

        setHasOptionsMenu(true);
        configureAsSubFragment();

        presenter.refreshContact();
    }


    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ButterKnife.bind(this, view as  View);
        adapter.view = this;
    }

    override fun onDestroyView() {
        adapter.view = null;
        super.onDestroyView()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_contact_details, container, false)
                ?: throw IllegalStateException("Could not create view")
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        //TODO
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        //TODO
        return true;
    }

    override val title: String
        get() = (if (injectCompleted) presenter.title else "")

    override fun supportBackButton(): Boolean = true

    override fun loadContact(contact: ContactDto) {
        adapter.loadContact(contact);
    }
}

