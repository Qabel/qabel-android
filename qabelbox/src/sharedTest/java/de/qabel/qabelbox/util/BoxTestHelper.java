package de.qabel.qabelbox.util;


import android.content.Context;

import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.persistence.RepositoryFactory;
import de.qabel.qabelbox.providers.DocumentIdParser;
import de.qabel.qabelbox.storage.AndroidBoxManager;
import de.qabel.qabelbox.storage.BoxManager;
import de.qabel.qabelbox.storage.notifications.AndroidStorageNotificationManager;
import de.qabel.qabelbox.storage.notifications.AndroidStorageNotificationPresenter;
import de.qabel.qabelbox.storage.transfer.FakeTransferManager;

public class BoxTestHelper {

    public static BoxManager createBoxManager(Context context) {
        RepositoryFactory repositoryFactory = new RepositoryFactory(context);
        return new AndroidBoxManager(context,
                new AndroidStorageNotificationManager(new AndroidStorageNotificationPresenter(context)),
                new DocumentIdParser(),
                new AppPreference(context),
                new FakeTransferManager(context.getExternalCacheDir()),
                repositoryFactory.getIdentityRepository(repositoryFactory.getAndroidClientDatabase()));
    }
}
