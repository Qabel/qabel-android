package de.qabel.qabelbox.chat.view.views

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.vanniktech.emoji.EmojiPopup
import de.qabel.core.config.Contact
import de.qabel.core.config.Identity
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.R
import de.qabel.qabelbox.base.BaseFragment
import de.qabel.qabelbox.box.openIntent
import de.qabel.qabelbox.box.provider.ShareId
import de.qabel.qabelbox.chat.dagger.ChatModule
import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.view.adapters.ChatMessageAdapter
import de.qabel.qabelbox.chat.view.presenters.ChatPresenter
import de.qabel.qabelbox.dagger.components.ActiveIdentityComponent
import de.qabel.qabelbox.helper.Helper
import de.qabel.qabelbox.ui.HeaderDecoration
import de.qabel.qabelbox.viewer.ImageViewerActivity
import kotlinx.android.synthetic.main.fragment_contact_chat.*
import kotlinx.android.synthetic.main.fragment_contact_chat.view.*
import org.jetbrains.anko.*
import java.net.URLConnection
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

    val adapter: ChatMessageAdapter = ChatMessageAdapter({ presenter.handleMsgClick(it) })
    var headerDecor: HeaderDecoration? = null
    lateinit override var contactKeyId: String
    @Inject
    lateinit var presenter: ChatPresenter
    @Inject
    lateinit var identity: Identity

    lateinit var emojiPopup: EmojiPopup

    val textWatcher = object : TextWatcher {
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            toggleSendButton()
        }

        override fun afterTextChanged(text: Editable?) {}
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        contactKeyId = arguments.getString(ARG_CONTACT) ?: throw IllegalArgumentException(
                "Starting ChatFragment without contactKeyId")
        val component = getComponent(ActiveIdentityComponent::class.java).plus(ChatModule(this))
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
        emojiPopup.dismiss()

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

        bt_send.onClick { presenter.sendMessage() }
        toggleSendButton()
        etText.addTextChangedListener(textWatcher)
        action_add_contact.setOnClickListener { presenter.handleContactAddClick() }
        action_ignore_contact.setOnClickListener { presenter.handleContactIgnoreClick() }
        emojiPopup = EmojiPopup.Builder.fromRootView(chat_root)
                .setOnEmojiPopupShownListener { emoji_popup.imageResource = R.drawable.ic_keyboard_grey_24dp }
                .setOnEmojiPopupDismissListener { emoji_popup.imageResource = R.drawable.emoji_people }
                .setOnSoftKeyboardCloseListener { emojiPopup.dismiss() }
                .build(etText)
        emoji_popup.onClick {
            emojiPopup.toggle()
            chat_root.viewTreeObserver.dispatchOnGlobalLayout()
        }
    }


    fun toggleSendButton() {
        if (messageText.isBlank()) {
            bt_send.isEnabled = false
            bt_send.setImageResource(R.drawable.ic_send_grey_24dp)
        } else {
            bt_send.isEnabled = true
            bt_send.setImageResource(R.drawable.ic_send_grey_active_24dp)
        }
    }

    override fun onBackPressed(): Boolean {
        if (emojiPopup.isShowing) {
            emojiPopup.dismiss()
            return true
        }
        return false
    }

    override fun reset() {
        busy()
        runOnUiThread {
            adapter.reset()
            swipeRefresh.isEnabled = true
            idle()
        }
    }

    override fun appendData(models: List<ChatMessage>) {
        busy()
        runOnUiThread {
            adapter.append(models)
            scrollToBottom()
            idle()
        }
    }

    override fun prependData(models: List<ChatMessage>) {
        debug("Showing ${models.size} messages")
        busy()
        runOnUiThread {
            val loadMore = adapter.itemCount > 0
            adapter.prepend(models)
            swipeRefresh.isRefreshing = false
            swipeRefresh.isEnabled = presenter.proxy.canLoadMore()
            contact_chat_list.removeItemDecoration(headerDecor)
            if (swipeRefresh.isEnabled) {
                contact_chat_list.addItemDecoration(headerDecor)
            }
            if (!loadMore) {
                scrollToBottom()
            }
            idle()
        }
    }

    override fun refreshItem(msg: ChatMessage) =
            runOnUiThread {
                adapter.notifyViewItem(msg)
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

    override fun sendMessageStateChange() {
        mActivity?.ctx?.sendBroadcast(Intent(QblBroadcastConstants.Chat.MESSAGE_STATE_CHANGED))
    }

    override fun showError(error: Throwable) {
        runOnUiThread {
            longToast(getString(R.string.error_saving_changed))
            error("Error in ChatFragment", error)
            refreshContactOverlay()
        }
    }

    override fun openShare(shareId: ShareId) {
        runOnUiThread {
            info("Open With via started for share id $shareId")
            val uri = shareId.toUri()
            val mimeType = URLConnection.guessContentTypeFromName(uri.toString()) ?: ""
            if (mimeType.startsWith("image")) {
                val intent = Intent(ctx, ImageViewerActivity::class.java).apply {
                    putExtra(ImageViewerActivity.P_URI, uri)
                    putExtra(ImageViewerActivity.P_TYPE, mimeType)
                }
                startActivity(intent)
            } else {
                uri.openIntent(ctx)
            }
        }
    }

}

