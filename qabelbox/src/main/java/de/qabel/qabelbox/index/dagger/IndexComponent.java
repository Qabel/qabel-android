package de.qabel.qabelbox.index.dagger;

import dagger.Subcomponent;
import de.qabel.qabelbox.index.AndroidIndexSyncService;
import de.qabel.qabelbox.index.ContactSyncAdapter;

@Subcomponent
public interface IndexComponent {

    void inject(AndroidIndexSyncService androidIndexSyncService);
    void inject(ContactSyncAdapter contactSyncAdapter);

}
