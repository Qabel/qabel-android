package de.qabel.qabelbox.dagger.components;

import android.support.v7.app.AppCompatActivity;

import dagger.Subcomponent;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.dagger.modules.ActivityModule;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.fragments.ContactChatFragment;
import de.qabel.qabelbox.fragments.ContactFragment;

@ActivityScope
@Subcomponent(
        modules = ActivityModule.class
)
public interface ActivityComponent {

    AppCompatActivity activity();

    void inject(ContactChatFragment contactChatFragment);
    void inject(MainActivity activity);
    void inject(ContactFragment fragment);
}
