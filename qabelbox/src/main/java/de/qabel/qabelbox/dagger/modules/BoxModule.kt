package de.qabel.qabelbox.dagger.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import de.qabel.core.repository.ContactRepository
import de.qabel.core.repository.IdentityRepository
import de.qabel.qabelbox.box.interactor.*
import de.qabel.qabelbox.config.AppPreference
import de.qabel.qabelbox.storage.server.BlockServer
import javax.inject.Singleton

@Module
class BoxModule {

    @Singleton
    @Provides
    fun provideProviderUseCase(useCase: BoxDocumentIdAdapter): DocumentIdAdapter {
        return useCase
    }

    @Singleton
    @Provides
    fun provideVolumeManager(identityRepository: IdentityRepository,
                             contactRepository: ContactRepository,
                             preference: AppPreference,
                             context: Context, blockServer: BlockServer):
            VolumeManager {
        return BoxVolumeManager(identityRepository, makeFileBrowserFactory(
                identityRepository, contactRepository, preference.deviceId, context.cacheDir, blockServer))
    }

}
