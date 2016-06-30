package de.qabel.qabelbox.dagger.components;

import dagger.Subcomponent;
import de.qabel.qabelbox.dagger.modules.FileBrowserModule;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;
import de.qabel.qabelbox.ui.views.FileBrowserFragment;

@ActivityScope
@Subcomponent(
        modules = FileBrowserModule.class
)
public interface FileBrowserComponent {
    void inject(FileBrowserFragment chatFragment);
}
