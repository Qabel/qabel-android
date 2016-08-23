package de.qabel.qabelbox.chat.view.views

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.R
import de.qabel.qabelbox.chat.dagger.ChatModule
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.view.adapters.ChatMessageAdapter
import de.qabel.qabelbox.chat.view.presenters.ChatPresenter
import de.qabel.qabelbox.dagger.components.MainActivityComponent
import de.qabel.qabelbox.fragments.BaseFragment
import de.qabel.qabelbox.helper.Helper
import de.qabel.qabelbox.ui.HeaderDecoration
import kotlinx.android.synthetic.main.fragment_contact_chat.*
import kotlinx.android.synthetic.main.fragment_contact_chat.view.*
import org.jetbrains.anko.*
import javax.inject.Inject

class ChatFragment : ChatView, BaseFragment(), AnkoLogger {

    override var messageText: String
        get() = view?.etText?.text.toString()
        set(value) {
            view?.etText?.setText(value)
        }


    override val isFabNeeded: Boolean = false

    var injectCompleted = false

    companion object {
        val ARG_CONTACT = "CONTACT"

        fun withContact(contact: Contact): ChatFragment {
            val fragment = ChatFragment()
            fragment.arguments = with(Bundle()) {
                putString(ARG_CONTACT, contact.keyIdentifier)
                this
            }
            return fragment
        }
    }

    val adapter: ChatMessageAdapter = ChatMessageAdapter()
    var headerDecor: HeaderDecoration? = null
    lateinit override var contactKeyId: String
    @Inject
    lateinit var presenter: ChatPresenter
    @Inject
    lateinit var identity: Identity

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contactKeyId = arguments.getString(ARG_CONTACT) ?: throw IllegalArgumentException(
                "Starting ChatFragment without contactKeyId")
        val component = getComponent(MainActivityComponent::class.java).plus(ChatModule(this))
        component.inject(this)
        injectCompleted = true
    }

    /**
     * Block notifications in which only the currently active contact
     * and identity are involved.
     */
    private val notificationBlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isOrderedBroadcast) {
                val ids = intent.getStringArrayListExtra(Helper.AFFECTED_IDENTITIES_AND_CONTACTS)
                        ?.filterNotNull() ?: return

                val currentKeys = listOf(contactKeyId, identity.keyIdentifier)
                if (ids.containsAll(currentKeys)) {
                    if (injectCompleted) {
                        presenter.refreshMessages()
                    }
                    if (ids.all { currentKeys.contains(it) }) {
                        abortBroadcast()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        configureAsSubFragment()
        ctx.registerReceiver(notificationBlockReceiver, IntentFilter(QblBroadcastConstants.Chat.NOTIFY_NEW_MESSAGES).apply {
            priority = 1
        })
        presenter.refreshMessages()
        refreshContactOverlay()
        mActivity?.toolbar?.setOnClickListener { presenter.handleHeaderClick() }
    }

    override fun refreshContactOverlay() {
        chat_contact_toolbar?.visibility = if (presenter.showContactMenu) View.VISIBLE else View.GONE
    }

    override fun onPause() {
        super.onPause()
        ctx.unregisterReceiver(notificationBlockReceiver)

        mActivity?.toolbar?.isClickable = false
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_contact_chat, container, false)
                ?: throw IllegalStateException("Could not create view")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(view.context)
        layoutManager.stackFromEnd = true
        contact_chat_list.layoutManager = layoutManager
        contact_chat_list.adapter = adapter
        headerDecor = HeaderDecoration.with(contact_chat_list)
                .inflate(R.layout.load_header)
                .parallax(0.4f)
                .dropShadowDp(1)
                .build()
        swipeRefresh.setOnRefreshListener {
            presenter.proxy.loadMore()
        }

        bt_send.setOnClickListener { presenter.sendMessage() }
        action_add_contact.setOnClickListener { presenter.handleContactAddClick() }
        action_ignore_contact.setOnClickListener { presenter.handleContactIgnoreClick() }
    }

    override fun reset() {
        busy()
        onUiThread {
            adapter.reset()
            swipeRefresh.isEnabled = true
            idle()
        }
    }

    override fun appendData(models: List<ChatMessage>) {
        busy()
        onUiThread {
            adapter.append(models)
            scrollToBottom()
            idle()
        }
    }

    override fun prependData(models: List<ChatMessage>) {
        debug("Showing ${models.size} messages")
        busy()
        onUiThread {
            adapter.prepend(models)
            swipeRefresh.isRefreshing = false
            swipeRefresh.isEnabled = presenter.proxy.canLoadMore()
            contact_chat_list.removeItemDecoration(headerDecor)
            if (swipeRefresh.isEnabled) {
                contact_chat_list.addItemDecoration(headerDecor)
            }
            idle()
        }
    }

    override fun handleLoadError(throwable: Throwable) = showError(throwable)

    override fun getCount(): Int = adapter.itemCount

    private fun scrollToBottom() {
        contact_chat_list.scrollToPosition(adapter.itemCount - 1)
    }

    override val title: String by lazy {
        if (injectCompleted) presenter.title else ""
    }

    override val subtitle: String? by lazy {
        if (injectCompleted && !presenter.subtitle.isEmpty()) presenter.subtitle else null
    }

    override fun supportBackButton(): Boolean = true

    override fun sendMessageStateChange() {
        mActivity?.ctx?.sendBroadcast(Intent(QblBroadcastConstants.Chat.MESSAGE_STATE_CHANGED))
    }

    override fun showError(error: Throwable) {
        onUiThread {
            longToast(getString(R.string.error_saving_changed))
            error("Error in ChatFragment", error)
            refreshContactOverlay()
        }
    }

}
