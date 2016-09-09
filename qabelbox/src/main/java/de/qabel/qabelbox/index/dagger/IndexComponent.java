package de.qabel.qabelbox.index.dagger;

import dagger.Subcomponent;
import de.qabel.qabelbox.index.AndroidIndexSyncService;

@Subcomponent(
        modules = IndexModule.class
)
public interface IndexComponent {

    void inject(AndroidIndexSyncService androidIndexSyncService);

}
