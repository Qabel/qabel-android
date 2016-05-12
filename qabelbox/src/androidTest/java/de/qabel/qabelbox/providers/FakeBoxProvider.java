package de.qabel.qabelbox.providers;

import android.support.annotation.NonNull;

import java.io.File;

import de.qabel.qabelbox.storage.FakeTransferManager;
import de.qabel.qabelbox.storage.TransferManager;

public class FakeBoxProvider extends BoxProvider {

    @NonNull
    @Override
    protected TransferManager createTransferManager(File tempDir) {
        return new FakeTransferManager(tempDir);
    }
}
