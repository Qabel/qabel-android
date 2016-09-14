package de.qabel.qabelbox.index.dagger;

import dagger.Subcomponent;
import de.qabel.qabelbox.index.AndroidIndexSyncService;

@Subcomponent
public interface IndexComponent {

    void inject(AndroidIndexSyncService androidIndexSyncService);

}
