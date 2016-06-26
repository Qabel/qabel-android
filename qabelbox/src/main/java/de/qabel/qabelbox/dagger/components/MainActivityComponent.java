package de.qabel.qabelbox.dagger.components;

import org.jetbrains.annotations.NotNull;

import dagger.Subcomponent;
import de.qabel.core.config.Identity;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.contacts.dagger.ContactDetailsComponent;
import de.qabel.qabelbox.contacts.dagger.ContactDetailsModule;
import de.qabel.qabelbox.contacts.dagger.ContactsComponent;
import de.qabel.qabelbox.contacts.dagger.ContactsModule;
import de.qabel.qabelbox.dagger.modules.ChatModule;
import de.qabel.qabelbox.dagger.modules.MainActivityModule;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.fragments.ContactChatFragment;
import de.qabel.qabelbox.fragments.ContactFragment;
import de.qabel.qabelbox.fragments.FilesFragment;
import de.qabel.qabelbox.ui.views.ChatFragment;

@ActivityScope
@Subcomponent(
        modules = MainActivityModule.class
)
public interface MainActivityComponent {

    void inject(MainActivity activity);
    MainActivity mainActivity();

    void inject(ContactFragment contactFragment);

    void inject(ContactChatFragment contactChatFragment);

    void inject(FilesFragment filesFragment);

    ChatComponent plus(ChatModule chatModule);

    ContactsComponent plus(ContactsModule contactsModule);
    ContactDetailsComponent plus(ContactDetailsModule contactDetailsModule);

}

