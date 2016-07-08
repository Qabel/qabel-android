package de.qabel.qabelbox.box.dagger.modules;

import dagger.Subcomponent;
import de.qabel.qabelbox.box.dagger.modules.FileBrowserModule;
import de.qabel.qabelbox.box.views.FileBrowserFragment;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;

@ActivityScope
@Subcomponent(
        modules = FileBrowserModule.class
)
public interface FileBrowserComponent {
    void inject(FileBrowserFragment chatFragment);
}
