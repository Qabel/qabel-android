package de.qabel.qabelbox.dagger.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import de.qabel.desktop.repository.IdentityRepository
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
    fun provideVolumeManager(identityRepository: IdentityRepository, preference: AppPreference,
                             context: Context, blockServer: BlockServer):
            VolumeManager {
        return BoxVolumeManager(identityRepository, makeFileBrowserFactory(
                identityRepository, preference.deviceId, context.cacheDir, blockServer))
    }

}
