package de.qabel.qabelbox.chat.dagger

import dagger.Module
import dagger.Provides
import de.qabel.qabelbox.chat.view.presenters.MainTextShareReceiverPresenter
import de.qabel.qabelbox.chat.view.presenters.TextShareReceiverPresenter
import de.qabel.qabelbox.chat.view.views.TextShareReceiver
import de.qabel.qabelbox.dagger.scopes.ActivityScope

@ActivityScope
@Module
class TextShareReceiverModule(val view: TextShareReceiver) {


    @Provides @ActivityScope
    fun provideView(): TextShareReceiver = view

    @Provides @ActivityScope
    fun providePresenter(mainTextShareReceiverPresenter: MainTextShareReceiverPresenter)
            : TextShareReceiverPresenter = mainTextShareReceiverPresenter

}
