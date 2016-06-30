package de.qabel.qabelbox.dagger.modules;

import android.content.Context;

import java.io.File;

import dagger.Module;
import de.qabel.qabelbox.config.AppPreference;
import de.qabel.qabelbox.storage.server.BlockServer;
import de.qabel.qabelbox.storage.server.MockBlockServer;

@Module
public class MockStorageModule extends StorageModule {


    @Override
    protected BlockServer createBlockServer(AppPreference preference, Context context) {
        return new MockBlockServer();
    }
}
