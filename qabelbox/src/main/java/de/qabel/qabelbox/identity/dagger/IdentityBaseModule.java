package de.qabel.qabelbox.identity.dagger;

import dagger.Module;
import dagger.Provides;
import de.qabel.qabelbox.identity.interactor.IdentityUseCase;
import de.qabel.qabelbox.identity.interactor.MainIdentityUseCase;

@Module
public abstract class IdentityBaseModule {

    @Provides
    public IdentityUseCase providesIdentityUseCase(MainIdentityUseCase useCase) {
        return useCase;
    }

}
