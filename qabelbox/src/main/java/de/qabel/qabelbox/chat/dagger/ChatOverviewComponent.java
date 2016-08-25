package de.qabel.qabelbox.chat.dagger;

import dagger.Subcomponent;
import de.qabel.qabelbox.chat.view.views.ChatOverviewFragment;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;

@ActivityScope
@Subcomponent(
        modules = ChatOverviewModule.class
)
public interface ChatOverviewComponent {
    void inject(ChatOverviewFragment chatFragment);
}
