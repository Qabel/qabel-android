package de.qabel.qabelbox.sync

import com.google.firebase.messaging.FirebaseMessaging
import de.qabel.core.drop.DropURL

class FirebaseTopicManager: TopicManager {
    private fun toTopic(dropUrl: DropURL) = dropUrl.toString().split("/").last()

    override fun subscribe(dropUrl: DropURL) {
        FirebaseMessaging.getInstance().subscribeToTopic(toTopic(dropUrl))
    }

    override fun unSubscribe(dropUrl: DropURL) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(toTopic(dropUrl))
    }

}

