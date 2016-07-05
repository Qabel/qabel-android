package de.qabel.qabelbox.contacts.dagger;

import dagger.Subcomponent;
import de.qabel.qabelbox.contacts.view.views.ContactDetailsFragment;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;

@ActivityScope
@Subcomponent(
        modules = ContactDetailsModule.class
)
public interface ContactDetailsComponent {

    void inject(ContactDetailsFragment contactDetailsFragment);

}
