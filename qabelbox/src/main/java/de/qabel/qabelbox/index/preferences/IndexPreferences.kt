package de.qabel.qabelbox.index.preferences

interface IndexPreferences {

    val contactSyncAsked : Boolean
    var contactSyncEnabled : Boolean
    var contactSyncTime : Long

    var phoneStatePermission : Boolean

}
