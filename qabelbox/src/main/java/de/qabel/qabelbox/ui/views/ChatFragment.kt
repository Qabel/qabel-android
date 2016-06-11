package de.qabel.qabelbox.ui.views

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
import de.qabel.qabelbox.R
import de.qabel.qabelbox.adapter.ChatMessageAdapter
import de.qabel.qabelbox.dagger.components.MainActivityComponent
import de.qabel.qabelbox.dagger.modules.ChatModule
import de.qabel.qabelbox.dto.ChatMessage
import de.qabel.qabelbox.fragments.BaseFragment
import de.qabel.qabelbox.helper.Helper
import de.qabel.qabelbox.ui.presenters.ChatPresenter
import kotlinx.android.synthetic.main.fragment_contact_chat.*
import kotlinx.android.synthetic.main.fragment_contact_chat.view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.warn
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
            val bundle = Bundle()
            bundle.putString(ARG_CONTACT, contact.keyIdentifier)
            fragment.arguments = bundle
            return fragment
        }
    }

    val adapter = ChatMessageAdapter(listOf())

    lateinit override var contactKeyId: String
    @Inject
    lateinit var presenter: ChatPresenter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contactKeyId = arguments.getString(ARG_CONTACT)?: throw IllegalArgumentException(
                "Starting ChatFragment without contactKeyId")
        val component = getComponent(MainActivityComponent::class.java).plus(ChatModule(this))
        component.inject(this);
        injectCompleted = true
        bt_send.setOnClickListener { presenter.sendMessage() }

        contact_chat_list.layoutManager = LinearLayoutManager(view.context)
        contact_chat_list.adapter = adapter
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (injectCompleted) {
                presenter.refreshMessages()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity.registerReceiver(broadcastReceiver, IntentFilter(Helper.INTENT_REFRESH_CHAT))
    }

    override fun onStop() {
        super.onStop()
        activity.unregisterReceiver(broadcastReceiver)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater?.inflate(R.layout.fragment_contact_chat, container, false)
                ?: throw IllegalStateException("Could not create view")
        return view
    }

    override fun showEmpty() {
        adapter.messages = listOf()
        adapter.notifyDataSetChanged()
    }

    override fun refresh() {
        val intent = Intent(Helper.INTENT_REFRESH_CONTACTLIST);
        activity?.sendBroadcast(intent, null);
    }

    override fun showMessages(messages: List<ChatMessage>) {
        warn("Showing ${messages.size} messages")
        fillAdapter(messages);
    }

    private fun fillAdapter(messages: List<ChatMessage>) {
        warn("Filling adapter with ${messages.size} messages")
        adapter.messages = messages
        adapter.notifyDataSetChanged()
    }

    override fun getTitle(): String? = if (injectCompleted) presenter.title else { "" }

}
