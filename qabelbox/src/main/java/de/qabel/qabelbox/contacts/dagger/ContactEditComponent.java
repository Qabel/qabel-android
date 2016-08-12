package de.qabel.qabelbox.contacts.dagger;

import dagger.Subcomponent;
import de.qabel.core.config.Contact;
import de.qabel.qabelbox.contacts.view.views.ContactDetailsFragment;
import de.qabel.qabelbox.contacts.view.views.ContactEditFragment;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;

@ActivityScope
@Subcomponent(
        modules = ContactEditModule.class
)
public interface ContactEditComponent {

    void inject(ContactEditFragment contactEditFragment);

}
