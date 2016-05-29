package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import java.io.File;

import de.qabel.qabelbox.storage.transfer.FakeTransferManager;
import de.qabel.qabelbox.storage.transfer.TransferManager;


public class MockStorageModule extends StorageModule {

    @Override
    File providesCacheDir(Context context) {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    @Override
    TransferManager providesTransferManager(Context context, File tmpFile) {
        return new FakeTransferManager(tmpFile);
    }
}
