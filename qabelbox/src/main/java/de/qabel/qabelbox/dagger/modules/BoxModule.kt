package de.qabel.qabelbox.dagger.modules

import dagger.Module
import dagger.Provides
import de.qabel.qabelbox.box.interactor.BoxDocumentIdAdapter
import de.qabel.qabelbox.box.interactor.DocumentIdAdapter
import javax.inject.Singleton

@Module
class BoxModule {

    @Singleton
    @Provides
    fun provideProviderUseCase(useCase: BoxDocumentIdAdapter): DocumentIdAdapter {
        return useCase
    }

}
