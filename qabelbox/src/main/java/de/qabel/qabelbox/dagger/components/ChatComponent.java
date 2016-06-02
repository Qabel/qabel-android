package de.qabel.qabelbox.dagger.components;

import dagger.Subcomponent;
import de.qabel.qabelbox.dagger.modules.ChatModule;
import de.qabel.qabelbox.ui.views.ChatFragment;

@Subcomponent(
        modules = ChatModule.class
)
public interface ChatComponent {
    void inject(ChatFragment chatFragment);
}
