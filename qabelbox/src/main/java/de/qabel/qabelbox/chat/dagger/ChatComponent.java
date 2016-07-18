package de.qabel.qabelbox.chat.dagger;

import dagger.Subcomponent;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.chat.view.views.ChatFragment;

@ActivityScope
@Subcomponent(
        modules = ChatModule.class
)
public interface ChatComponent {
    void inject(ChatFragment chatFragment);
}
