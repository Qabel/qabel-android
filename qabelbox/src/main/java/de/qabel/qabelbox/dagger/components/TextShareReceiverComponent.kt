package de.qabel.qabelbox.dagger.components

import dagger.Subcomponent
import de.qabel.qabelbox.chat.dagger.TextShareReceiverModule
import de.qabel.qabelbox.chat.view.views.TextShareReceiverActivity
import de.qabel.qabelbox.dagger.scopes.ActivityScope

@ActivityScope
@Subcomponent(modules = arrayOf(TextShareReceiverModule::class))
interface TextShareReceiverComponent {
    fun inject(textShareReceiverActivity: TextShareReceiverActivity)
}

