package de.qabel.qabelbox.dagger.components;

import dagger.Subcomponent;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.chat.dagger.ChatComponent;
import de.qabel.qabelbox.chat.dagger.ChatOverviewComponent;
import de.qabel.qabelbox.chat.dagger.ChatOverviewModule;
import de.qabel.qabelbox.contacts.dagger.ContactDetailsComponent;
import de.qabel.qabelbox.contacts.dagger.ContactDetailsModule;
import de.qabel.qabelbox.contacts.dagger.ContactEditComponent;
import de.qabel.qabelbox.contacts.dagger.ContactEditModule;
import de.qabel.qabelbox.contacts.dagger.ContactsComponent;
import de.qabel.qabelbox.contacts.dagger.ContactsModule;
import de.qabel.qabelbox.chat.dagger.ChatModule;
import de.qabel.qabelbox.dagger.modules.FileBrowserModule;
import de.qabel.qabelbox.dagger.modules.MainActivityModule;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.fragments.IdentitiesFragment;
import de.qabel.qabelbox.identity.dagger.IdentityDetailsComponent;
import de.qabel.qabelbox.identity.dagger.IdentityDetailsModule;

@ActivityScope
@Subcomponent(
        modules = MainActivityModule.class
)
public interface MainActivityComponent {

    void inject(MainActivity activity);

    ChatComponent plus(ChatModule chatModule);
    ChatOverviewComponent plus(ChatOverviewModule chatOverviewModule);
    FileBrowserComponent plus(FileBrowserModule fileBrowserModule);

    ContactsComponent plus(ContactsModule contactsModule);
    ContactDetailsComponent plus(ContactDetailsModule contactDetailsModule);
    ContactEditComponent plus(ContactEditModule contactEditModule);

    IdentityDetailsComponent plus(IdentityDetailsModule identityDetailsModule);

    void inject(IdentitiesFragment identitiesFragment);

}

