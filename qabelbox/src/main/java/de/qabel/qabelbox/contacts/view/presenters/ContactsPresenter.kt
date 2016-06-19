package de.qabel.qabelbox.contacts.view.presenters

import android.app.Activity
import android.content.Intent
import de.qabel.qabelbox.contacts.dto.ContactDto

interface ContactsPresenter {

    fun refresh()

    fun deleteContact(activity : Activity, contact : ContactDto)

    fun exportContact(contact : ContactDto)

    fun sendContact(activity : Activity, contact : ContactDto)

    fun handleActivityResult(activity: Activity, requestCode: Int, resultData: Intent?)

}
