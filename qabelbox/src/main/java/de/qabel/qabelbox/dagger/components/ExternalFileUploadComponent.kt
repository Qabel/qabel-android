package de.qabel.qabelbox.dagger.components

import dagger.Subcomponent
import de.qabel.qabelbox.box.views.ExternalFileUploadActivity
import de.qabel.qabelbox.dagger.modules.ExternalFileUploadModule
import de.qabel.qabelbox.dagger.scopes.ActivityScope

@ActivityScope
@Subcomponent(modules = arrayOf(ExternalFileUploadModule::class))
interface ExternalFileUploadComponent {
    fun  inject(externalFileUploadActivity: ExternalFileUploadActivity)

}

