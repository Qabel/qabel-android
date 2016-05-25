package de.qabel.qabelbox.dagger.components;

import dagger.Subcomponent;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.dagger.modules.MainActivityModule;
import de.qabel.qabelbox.fragments.ContactChatFragment;
import de.qabel.qabelbox.fragments.ContactFragment;

@Subcomponent(
        modules = MainActivityModule.class
)
public interface MainActivityComponent {

    void inject(MainActivity activity);
    MainActivity mainActivity();

    void inject(ContactFragment contactFragment);

    void inject(ContactChatFragment contactChatFragment);
}
