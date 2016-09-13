package de.qabel.qabelbox.identity.dagger;

import dagger.Subcomponent;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.identity.view.IdentityDetailsFragment;

@ActivityScope
@Subcomponent(
        modules = IdentityDetailsModule.class
)
public interface IdentityDetailsComponent {

    void inject(IdentityDetailsFragment identityFragment);

}
