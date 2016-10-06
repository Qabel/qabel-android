package de.qabel.qabelbox.contacts.view.views

import android.os.Bundle
import android.view.*
import butterknife.ButterKnife
import de.qabel.core.config.Identities
import de.qabel.qabelbox.R
import de.qabel.qabelbox.contacts.dagger.ContactEditModule
import de.qabel.qabelbox.contacts.dto.ContactDto
import de.qabel.qabelbox.contacts.view.adapters.ContactEditAdapter
import de.qabel.qabelbox.contacts.view.presenters.ContactEditPresenter
import de.qabel.qabelbox.dagger.components.MainActivityComponent
import de.qabel.qabelbox.fragments.BaseFragment
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.ctx
import org.jetbrains.anko.toast
import javax.inject.Inject


class ContactEditFragment() : ContactEditView, BaseFragment(showOptionsMenu = true), AnkoLogger {

    companion object {
        private val ARG_CONTACT = "CONTACT"

        fun withContact(contact: ContactDto): ContactEditFragment {
            val fragment = ContactEditFragment()
            fragment.arguments = with(Bundle()) {
                putSerializable(ARG_CONTACT, contact)
                this
            }
            return fragment
        }
    }

    val adapter = ContactEditAdapter()
    lateinit override var contactDto: ContactDto
    @Inject lateinit var presenter: ContactEditPresenter

    var injectCompleted = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contactDto = arguments.getSerializable(ARG_CONTACT) as ContactDto? ?: throw IllegalArgumentException(
                "Starting ContactEditFragment without contact"
        )

        val component = getComponent(MainActivityComponent::class.java).plus(ContactEditModule(this))
        component.inject(this)
        injectCompleted = true

        presenter.loadContact()
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
        return inflater?.inflate(R.layout.fragment_contact_edit, container, false)
                ?: throw IllegalStateException("Could not create view")
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.ab_contact_edit, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_contact_save -> presenter.onSaveClick()
        }
        return true
    }

    override val title: String
        get() = (if (injectCompleted) presenter.title else "")

    override fun loadContact(contact: ContactDto, identities: Identities) {
        adapter.loadContact(contact, identities)
    }

    override fun getEditLabel(): String = ctx.getString(R.string.contact_edit)
    override fun getNewLabel(): String = ctx.getString(R.string.contact_new)

    override fun getCurrentNick(): String = adapter.getNickname()
    override fun getCurrentIdentityIds(): List<Int> = adapter.getIdentityIds()
    override fun isContactIgnored(): Boolean = adapter.isContactIgnored()

    override fun showEnterNameToast() =
            toast(R.string.enter_name_message)

    override fun showContactSavedToast() = toast(R.string.changes_saved)
    override fun showSaveFailed() = toast(R.string.error_saving_changed)

}

