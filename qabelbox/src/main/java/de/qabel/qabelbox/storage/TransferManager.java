package de.qabel.qabelbox.storage;

import android.support.annotation.Nullable;

import java.io.File;

public interface TransferManager {
    File createTempFile();

    int uploadAndDeleteLocalfileOnSuccess(String prefix, String name, File localfile, @Nullable BoxTransferListener boxTransferListener);

    Exception lookupError(int transferId);

    int download(String prefix, String name, File file, @Nullable BoxTransferListener boxTransferListener);

    boolean waitFor(int id);

    int delete(String prefix, String name);
}
