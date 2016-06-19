package de.qabel.qabelbox.contacts.dagger;

import dagger.Subcomponent;
import de.qabel.qabelbox.contacts.view.ContactsFragment;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;

@ActivityScope
@Subcomponent(
        modules = ContactsModule.class
)
public interface ContactsComponent {

    void inject(ContactsFragment contactsFragment);

}
