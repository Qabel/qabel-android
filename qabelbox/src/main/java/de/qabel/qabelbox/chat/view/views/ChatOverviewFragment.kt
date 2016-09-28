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
import butterknife.ButterKnife
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
import org.jetbrains.anko.onUiThread
import javax.inject.Inject

class ChatOverviewFragment() : ChatOverview, BaseFragment(), AnkoLogger {

    var injectCompleted = false

    @Inject
    lateinit var presenter: ChatOverviewPresenter

    @Inject
    override lateinit var identity: Identity

    override val title: String by lazy { ctx.getString(R.string.conversations) }
    override val isFabNeeded = true

    val adapter = ChatOverviewAdapter({
        contact -> presenter.handleClick(contact)
    }, {
        contact -> presenter.handleLongClick(contact)
    })

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
        ButterKnife.bind(this, view as View)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (injectCompleted) {
                presenter.refresh()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setHasOptionsMenu(false)
        configureAsMainFragment()
        presenter.refresh()
        ctx.registerReceiver(broadcastReceiver, IntentFilter(QblBroadcastConstants.Chat.NOTIFY_NEW_MESSAGES).apply {
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
        onUiThread {
            adapter.init(data)
            adapter.notifyDataSetChanged()
            updateView(data.size)
            idle()
        }
    }

}
