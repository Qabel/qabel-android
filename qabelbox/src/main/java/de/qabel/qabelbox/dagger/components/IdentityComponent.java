package de.qabel.qabelbox.dagger.components;

import dagger.Subcomponent;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.dagger.modules.IdentityModule;
import de.qabel.qabelbox.dagger.scopes.IdentityScope;

@IdentityScope
@Subcomponent(
        modules = IdentityModule.class
)
public interface IdentityComponent {
    void inject(Object object);

    Identity identity();
}
