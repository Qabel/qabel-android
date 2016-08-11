package de.qabel.qabelbox.sync

import de.qabel.core.drop.DropURL

interface TopicManager {

    fun subscribe(dropUrl: DropURL)
    fun unSubscribe(dropUrl: DropURL)
}

