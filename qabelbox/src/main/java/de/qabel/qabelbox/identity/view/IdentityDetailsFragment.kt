package de.qabel.qabelbox.identity.view

import android.os.Bundle
import android.text.InputType
import android.view.*
import butterknife.ButterKnife
import de.qabel.core.config.Identity
import de.qabel.qabelbox.R
import de.qabel.qabelbox.dagger.components.MainActivityComponent
import de.qabel.qabelbox.fragments.BaseFragment
import de.qabel.qabelbox.identity.dagger.IdentityDetailsModule
import de.qabel.qabelbox.identity.view.adapter.IdentityDetailsAdapter
import de.qabel.qabelbox.identity.view.presenter.IdentityDetailsPresenter
import de.qabel.qabelbox.ui.extensions.showEnterTextDialog
import kotlinx.android.synthetic.main.fragment_identity_details.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.ctx
import org.jetbrains.anko.toast
import javax.inject.Inject


class IdentityDetailsFragment() : IdentityDetailsView, BaseFragment(), AnkoLogger {

    companion object {
        private val ARG_IDENTITY = "IDENTITY"

        fun withIdentity(identity: Identity): IdentityDetailsFragment {
            val fragment = IdentityDetailsFragment()
            fragment.arguments = with(Bundle()) {
                putString(ARG_IDENTITY, identity.keyIdentifier)
                this
            }
            return fragment
        }
    }

    val adapter = IdentityDetailsAdapter()
    lateinit override var identityKeyId: String
    @Inject lateinit var presenter: IdentityDetailsPresenter

    var injectCompleted = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        identityKeyId = arguments.getString(ARG_IDENTITY) ?: throw IllegalArgumentException(
                "Starting IdentityDetailsFragment without identityKeyId")

        val component = getComponent(MainActivityComponent::class.java).plus(IdentityDetailsModule(this))
        component.inject(this)
        injectCompleted = true

        setHasOptionsMenu(false)
        configureAsSubFragment()

        presenter.loadIdentity()
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ButterKnife.bind(this, view as View)
        adapter.view = view
        editIdentityAlias.setOnClickListener({
            showEnterTextDialog(R.string.create_identity_enter_name, R.string.enter_name_message, InputType.TYPE_CLASS_TEXT, {
                alias ->
                presenter.onSaveAlias(alias)
            }, editIdentityAlias.text)
        })
        editIdentityEmail.setOnClickListener({
            showEnterTextDialog(R.string.create_identity_enter_name, R.string.enter_name_message, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, {
                email ->
                presenter.onSaveEmail(email)
            }, editIdentityEmail.text, R.string.qabel_index_text)
        })
        editIdentityPhone.setOnClickListener({
            showEnterTextDialog(R.string.create_identity_enter_name, R.string.enter_name_message, InputType.TYPE_CLASS_PHONE, {
                phone ->
                presenter.onSavePhoneNumber(phone)
            }, editIdentityPhone.text, R.string.qabel_index_text)
        })

        action_show_qr.setOnClickListener({
            presenter.onShowQRClick()
        })
    }

    override fun onDestroyView() {
        adapter.view = null
        super.onDestroyView()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_identity_details, container, false)
                ?: throw IllegalStateException("Could not create view")
    }

    override val title: String
        get() = (if (injectCompleted) ctx.getString(R.string.identity) else "")

    override fun supportBackButton(): Boolean = true

    override fun showEnterNameToast() = toast(R.string.enter_name_message)

    override fun showIdentitySavedToast() = toast(R.string.changes_saved)
    override fun showSaveFailed() = toast(R.string.error_saving_changed)

    override fun loadIdentity(identity: Identity) {
        adapter.loadIdentity(identity)
        refreshTitles()
    }

}

