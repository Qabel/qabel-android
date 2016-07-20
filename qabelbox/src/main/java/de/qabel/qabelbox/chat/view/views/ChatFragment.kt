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
import de.qabel.qabelbox.R
import de.qabel.qabelbox.chat.view.adapters.ChatMessageAdapter
import de.qabel.qabelbox.dagger.components.MainActivityComponent
import de.qabel.qabelbox.chat.dagger.ChatModule
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.fragments.BaseFragment
import de.qabel.qabelbox.helper.Helper
import de.qabel.qabelbox.chat.view.presenters.ChatPresenter
import kotlinx.android.synthetic.main.fragment_contact_chat.*
import kotlinx.android.synthetic.main.fragment_contact_chat.view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.ctx
import org.jetbrains.anko.debug
import org.jetbrains.anko.onUiThread
import javax.inject.Inject

class ChatFragment : ChatView, BaseFragment(), AnkoLogger {
    override var messageText: String
        get() = view?.etText?.text.toString()
        set(value) {
            view?.etText?.setText(value)
        }

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

    val adapter = ChatMessageAdapter(listOf())

    lateinit override var contactKeyId: String
    @Inject
    lateinit var presenter: ChatPresenter
    @Inject
    lateinit var identity: Identity

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contactKeyId = arguments.getString(ARG_CONTACT)?: throw IllegalArgumentException(
                "Starting ChatFragment without contactKeyId")
        val component = getComponent(MainActivityComponent::class.java).plus(ChatModule(this))
        component.inject(this);
        injectCompleted = true
        bt_send.setOnClickListener { presenter.sendMessage() }

        configureAsSubFragment();

        val layoutManager = LinearLayoutManager(view.context);
        layoutManager.stackFromEnd = true;
        contact_chat_list.layoutManager = layoutManager;
        contact_chat_list.adapter = adapter
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (injectCompleted) {
                presenter.refreshMessages()
            }
        }
    }

    /**
     * Block notifications in which only the currently active contact
     * and identity are involved.
     */
    private val notificationBlockReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isOrderedBroadcast) {
                val ids = intent?.getStringArrayListExtra(Helper.AFFECTED_IDENTITIES_AND_CONTACTS)
                        ?.filterNotNull() ?: return
                if (ids.all({(it == contactKeyId) or (it == identity.keyIdentifier)})) {
                    abortBroadcast();
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ctx.registerReceiver(broadcastReceiver, IntentFilter(Helper.INTENT_REFRESH_CHAT))
        ctx.registerReceiver(notificationBlockReceiver, IntentFilter(Helper.INTENT_SHOW_NOTIFICATION))
    }

    override fun onPause() {
        super.onPause()
        ctx.unregisterReceiver(broadcastReceiver)
        ctx.unregisterReceiver(notificationBlockReceiver)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_contact_chat, container, false)
                ?: throw IllegalStateException("Could not create view")
    }

    override fun showEmpty() {
        busy()
        onUiThread {
            adapter.messages = listOf()
            adapter.notifyDataSetChanged()
            idle()
        }
    }

    override fun refresh() {
        val intent = Intent(Helper.INTENT_REFRESH_CONTACTLIST);
        activity?.sendBroadcast(intent, null);
    }

    override fun showMessages(messages: List<ChatMessage>) {
        debug("Showing ${messages.size} messages")
        busy()
        onUiThread {
            fillAdapter(messages);
            idle()
        }
    }

    override fun appendMessage(message: ChatMessage) {
        busy()
        onUiThread {
            adapter.messages = adapter.messages + message
            adapter.notifyDataSetChanged();
            contact_chat_list.scrollToPosition(adapter.itemCount - 1)
            idle()
        }
    }


    private fun fillAdapter(messages: List<ChatMessage>) {
        debug("Filling adapter with ${messages.size} messages")
        adapter.messages = messages
        adapter.notifyDataSetChanged()
    }

    override fun getTitle(): String? = if (injectCompleted) presenter.title else { "" }

    override fun supportBackButton(): Boolean = true

}
