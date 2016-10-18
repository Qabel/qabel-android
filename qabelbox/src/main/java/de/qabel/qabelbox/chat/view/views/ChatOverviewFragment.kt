package de.qabel.qabelbox.chat.view.views

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import de.qabel.core.config.Identity
import de.qabel.qabelbox.QblBroadcastConstants
import de.qabel.qabelbox.R
import de.qabel.qabelbox.chat.dagger.ChatOverviewModule
import de.qabel.qabelbox.chat.dto.ChatConversationDto
import de.qabel.qabelbox.chat.view.adapters.ChatOverviewAdapter
import de.qabel.qabelbox.chat.view.presenters.ChatOverviewPresenter
import de.qabel.qabelbox.dagger.components.MainActivityComponent
import de.qabel.qabelbox.fragments.BaseFragment
import kotlinx.android.synthetic.main.fragment_contacts.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.ctx
import org.jetbrains.anko.runOnUiThread
import javax.inject.Inject

class ChatOverviewFragment() : ChatOverview, BaseFragment(
        mainFragment = true, showOptionsMenu = true, showFAButton = true), AnkoLogger {

    var injectCompleted = false

    @Inject
    lateinit var presenter: ChatOverviewPresenter

    @Inject
    override lateinit var identity: Identity

    override val title: String by lazy { ctx.getString(R.string.conversations) }

    val adapter = ChatOverviewAdapter({ presenter.handleClick(it) }, { presenter.handleLongClick(it) })

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val component = getComponent(MainActivityComponent::class.java).plus(ChatOverviewModule(this))
        component.inject(this)
        injectCompleted = true

        contact_list.layoutManager = LinearLayoutManager(view.context)
        contact_list.adapter = adapter
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (injectCompleted) {
                presenter.refresh()
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.ab_conversations, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.markAsRead) {
            presenter.markAllAsRead()
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        presenter.refresh()
        ctx.registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(QblBroadcastConstants.Chat.NOTIFY_NEW_MESSAGES)
            addAction(QblBroadcastConstants.Chat.MESSAGE_STATE_CHANGED)
            priority = 1
        })
    }

    override fun onPause() {
        super.onPause()
        ctx.unregisterReceiver(broadcastReceiver)
    }

    override fun handleFABAction(): Boolean {
        presenter.navigateToContacts()
        return true
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.fragment_chat_overview, container, false)
                ?: throw IllegalStateException("Could not create view")
    }

    private fun updateView(itemCount: Int) {
        empty_view?.visibility = if (itemCount == 0) View.VISIBLE else View.GONE
    }

    override fun loadData(data: List<ChatConversationDto>) {
        debug("Filling adapter with ${data.size} messages")
        busy()
        runOnUiThread {
            adapter.init(data)
            adapter.notifyDataSetChanged()
            updateView(data.size)
            idle()
        }
    }

}
