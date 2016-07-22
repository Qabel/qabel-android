package de.qabel.qabelbox.dagger.modules

import javax.inject.Singleton

import dagger.Module
import dagger.Provides
import de.qabel.core.config.Identity
import de.qabel.desktop.repository.IdentityRepository
import de.qabel.qabelbox.box.dto.VolumeRoot
import de.qabel.qabelbox.box.interactor.*

@Module
class BoxModule {

    @Singleton
    @Provides
    fun provideProviderUseCase(useCase: BoxProviderUseCase): ProviderUseCase {
        return useCase
    }

    @Singleton
    @Provides
    fun provideVolumeManager(identityRepository: IdentityRepository): VolumeManager {
        return BoxVolumeManager(identityRepository, makeFileBrowserFactory(identityRepository))
    }

}
