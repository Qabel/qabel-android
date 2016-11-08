package de.qabel.qabelbox.dagger.components;

import dagger.Subcomponent;
import de.qabel.qabelbox.box.views.FileBrowserFragment;
import de.qabel.qabelbox.box.views.FolderChooserActivity;
import de.qabel.qabelbox.dagger.modules.FileBrowserModule;
import de.qabel.qabelbox.dagger.modules.FolderChooserModule;
import de.qabel.qabelbox.dagger.scopes.ActivityScope;

@ActivityScope
@Subcomponent(
        modules = {FolderChooserModule.class, FileBrowserModule.class}
)
public interface FolderChooserComponent {
    void inject(FolderChooserActivity folderChooserActivity);
}
