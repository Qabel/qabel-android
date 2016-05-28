package de.qabel.qabelbox.storage.transfer;

import android.support.annotation.Nullable;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import de.qabel.qabelbox.exceptions.QblServerException;

public class FakeTransferManager implements TransferManager {

    private final File tempDir;
    private final Random random = new Random();

    static HashMap<String, File> uploads = new HashMap<>();
    static HashMap<Integer, Exception> errors = new HashMap<>();

    public FakeTransferManager(File tempDir) {
        this.tempDir = tempDir;
    }

    private String createKey(String prefix, String name) {
        return prefix + "/" + name;
    }

    @Override
    public File createTempFile() {

        try {
            return File.createTempFile("download", "", tempDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create tempfile");
        }
    }
    @Override
    public int uploadAndDeleteLocalfileOnSuccess(String prefix, String name, File localfile,
                                                 @Nullable BoxTransferListener boxTransferListener) {
        File storedFile = createTempFile();
        int id = random.nextInt();
        try {
            FileUtils.copyFile(localfile, storedFile);
            localfile.delete();
        } catch (IOException e) {
            errors.put(id, e);
            return id;
        }
        uploads.put(createKey(prefix, name), storedFile);
        if (boxTransferListener != null) {
            boxTransferListener.onFinished();
        }
        return id;
    }

    @Override
    public Exception lookupError(int transferId) {
        return errors.get(transferId);
    }

    @Override
    public int download(String prefix, String name, File file, @Nullable BoxTransferListener boxTransferListener) {
        File storedFile = uploads.get(createKey(prefix, name));
        int id = random.nextInt();
        if (storedFile == null) {
            errors.put(id, new QblServerException(404, "File not found"));
        } else {
            try {
                FileUtils.copyFile(storedFile, file);
            } catch (IOException e) {
                errors.put(id, new QblServerException(400, "Fake transfer manager"));
                return id;
            }
        }
        return id;
    }

    @Override
    public boolean waitFor(int id) {
        return !errors.containsKey(id);
    }

    @Override
    public int delete(String prefix, String name) {
        File remove = uploads.remove(createKey(prefix, name));
        if (remove != null) {
            remove.delete();
        }
        return 0;
    }
}
