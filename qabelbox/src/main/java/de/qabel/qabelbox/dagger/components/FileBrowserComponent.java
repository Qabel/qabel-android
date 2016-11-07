package de.qabel.qabelbox.dagger.components;

import dagger.Subcomponent;
import de.qabel.qabelbox.dagger.modules.FileBrowserModule;
import de.qabel.qabelbox.box.views.FileBrowserFragment;
import de.qabel.qabelbox.dagger.modules.FileBrowserViewModule;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;

@ActivityScope
@Subcomponent(
        modules = {FileBrowserModule.class, FileBrowserViewModule.class}
)
public interface FileBrowserComponent {
    void inject(FileBrowserFragment fileBrowserFragment);
}
