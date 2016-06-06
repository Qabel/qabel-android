package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import java.io.File;

import dagger.Module;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.storage.server.BlockServer;
import de.qabel.qabelbox.storage.server.MockBlockServer;
import de.qabel.qabelbox.storage.transfer.FakeTransferManager;
import de.qabel.qabelbox.storage.transfer.TransferManager;

@Module
public class MockStorageModule extends StorageModule {

    @Override
    public TransferManager createTransferManager(Context context, BlockServer blockServer, File tmpDir) {
        return new FakeTransferManager(context.getExternalCacheDir());
    }

    @Override
    protected BlockServer createBlockServer(AppPreference preference, Context context) {
        return new MockBlockServer();
    }
}
