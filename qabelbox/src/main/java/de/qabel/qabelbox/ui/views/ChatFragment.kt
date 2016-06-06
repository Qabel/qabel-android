package de.qabel.qabelbox.ui.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.ButterKnife
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

class ChatFragment : ChatView, BaseFragment() {

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

    val adapter = ChatMessageAdapter()

    lateinit override var contactKeyId: String
    lateinit var presenter: ChatPresenter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contactKeyId = savedInstanceState?.getString(ARG_CONTACT)?: ""
        val component = getComponent(MainActivityComponent::class.java).plus(ChatModule(this))
        component.inject(this);

        contact_chat_list.adapter = adapter
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater?.inflate(R.layout.fragment_contact_chat, container, false)
                ?: throw IllegalStateException("Could not create view")
        ButterKnife.bind(this, view)
        return view
    }

    override fun showEmpty() {
    }

    override fun showMessages(messages: List<ChatMessage>) {
        val intent = Intent(Helper.INTENT_REFRESH_CONTACTLIST);
        activity?.sendBroadcast(intent, null);
        fillAdapter(messages);
    }

    private fun fillAdapter(messages: List<ChatMessage>) {
        adapter.chatMessages = messages
        adapter.notifyDataSetChanged()
    }

    override fun getTitle(): String? = presenter.title
}
