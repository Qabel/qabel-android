package de.qabel.qabelbox.dagger.components;

import org.jetbrains.annotations.NotNull;

import dagger.Subcomponent;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.dagger.modules.ChatModule;
import de.qabel.qabelbox.dagger.modules.MainActivityModule;
import de.qabel.qabelbox.fragments.ContactChatFragment;
import de.qabel.qabelbox.fragments.ContactFragment;
import de.qabel.qabelbox.fragments.FilesFragment;
import de.qabel.qabelbox.ui.views.ChatFragment;

@Subcomponent(
        modules = MainActivityModule.class
)
public interface MainActivityComponent {

    void inject(MainActivity activity);
    MainActivity mainActivity();

    void inject(ContactFragment contactFragment);

    void inject(ContactChatFragment contactChatFragment);

    void inject(FilesFragment filesFragment);

    void inject(ChatFragment chatFragment);

    @NotNull
    ChatComponent plus(ChatModule chatModule);
}

