package de.qabel.qabelbox.dagger.modules;

import dagger.Module;
import dagger.Provides;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.QblECKeyPair;

@Module
public class IdentityModule {
    private final Identity identity;

    public IdentityModule(Identity identity) {
        this.identity = identity;
    }

    @Provides Identity provideIdentity() {
        return this.identity;
    }

    @Provides QblECKeyPair provideKeyPair(Identity identity) {
        return identity.getPrimaryKeyPair();
    }
}
