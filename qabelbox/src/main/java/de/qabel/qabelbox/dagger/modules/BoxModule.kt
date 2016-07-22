package de.qabel.qabelbox.dagger.modules

import javax.inject.Singleton

import dagger.Module
import dagger.Provides
import de.qabel.core.config.Identity
import de.qabel.desktop.repository.IdentityRepository
import de.qabel.qabelbox.box.backends.MockStorageBackend
import de.qabel.qabelbox.box.dto.VolumeRoot
import de.qabel.qabelbox.box.interactor.*

@Module
class BoxModule {

    @Singleton
    @Provides
    fun provideProviderUseCase(useCase: BoxProvider): Provider {
        return useCase
    }

    @Singleton
    @Provides
    fun provideVolumeManager(identityRepository: IdentityRepository): VolumeManager {
        val backend = MockStorageBackend()
        return BoxVolumeManager(identityRepository, makeFileBrowserFactory(
                identityRepository, byteArrayOf(1), backend, backend, createTempDir()))
    }

}
