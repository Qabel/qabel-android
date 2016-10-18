package de.qabel.qabelbox.chat.view.presenters

import de.qabel.qabelbox.chat.dto.ChatMessage
import de.qabel.qabelbox.chat.interactor.FindLatestConversations
import de.qabel.qabelbox.chat.view.views.ChatOverview
import de.qabel.qabelbox.navigation.Navigator
import javax.inject.Inject

class MainChatOverviewPresenter @Inject constructor(private val view: ChatOverview,
                                                    private val useCase: FindLatestConversations,
                                                    private val navigator: Navigator) : ChatOverviewPresenter {
    override fun refresh() {
        useCase.findLatest(view.identity).toList().subscribe({
            view.loadData(it)
        })
    }

    override fun handleClick(message: ChatMessage) {
        navigator.selectChatFragment(message.contact.keyIdentifier)
    }

    override fun handleLongClick(message: ChatMessage): Boolean = false

    override fun navigateToContacts() =
            navigator.selectContactsFragment()

}
