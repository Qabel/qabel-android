package de.qabel.qabelbox.util;


import android.content.Context;

import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.persistence.RepositoryFactory;
import de.qabel.qabelbox.providers.DocumentIdParser;
import de.qabel.qabelbox.storage.AndroidBoxManager;
import de.qabel.qabelbox.storage.BoxManager;
import de.qabel.qabelbox.storage.notifications.AndroidStorageNotificationManager;
import de.qabel.qabelbox.storage.notifications.AndroidStorageNotificationPresenter;
import de.qabel.qabelbox.storage.transfer.FakeTransferManager;

public class BoxTestHelper {

    private Context context;
    private RepositoryFactory repositoryFactory;

    public BoxTestHelper(Context context){
        this.context = context;
        this.repositoryFactory = new RepositoryFactory(context);
    }

    public BoxManager createBoxManager() {
        return new AndroidBoxManager(context,
                new AndroidStorageNotificationManager(new AndroidStorageNotificationPresenter(context)),
                new DocumentIdParser(),
                new AppPreference(context),
                new FakeTransferManager(context.getExternalCacheDir()),
                repositoryFactory.getIdentityRepository(repositoryFactory.getAndroidClientDatabase()));
    }

    public IdentityRepository createIdentityRepository(){
        RepositoryFactory repositoryFactory = new RepositoryFactory(context);
        return repositoryFactory.getIdentityRepository(repositoryFactory.getAndroidClientDatabase());
    }

    public AppPreference createAppPreferences(){
        return new AppPreference(context);
    }

    public ContactRepository createContactRepository(){
        return repositoryFactory.getContactRepository(repositoryFactory.getAndroidClientDatabase());
    }

}
