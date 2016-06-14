package de.qabel.qabelbox.storage.transfer;

import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Random;

import de.qabel.qabelbox.exceptions.QblServerException;

public class FakeTransferManager implements TransferManager {

    private static final String TAG = FakeTransferManager.class.getSimpleName();

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
            Log.d(TAG, "Stored File: " + createKey(prefix, name));
            copyWithProgress(localfile, storedFile, boxTransferListener);
            localfile.delete();
        } catch (IOException e) {
            Log.d(TAG, "Error storing file: " + createKey(prefix, name));
            errors.put(id, e);
            return id;
        }
        uploads.put(createKey(prefix, name), storedFile);
        if (boxTransferListener != null) {
            boxTransferListener.onFinished();
        }
        return id;
    }

    private void copyWithProgress(File source, File target, BoxTransferListener transferListener) throws IOException {
        InputStream inputStream = new FileInputStream(source);
        OutputStream outputStream = new FileOutputStream(target);
        byte[] buffer = new byte[4048];
        long total = source.length();
        long count = 0;
        int n;
        while (-1 != (n = inputStream.read(buffer))) {
            outputStream.write(buffer, 0, n);
            count += n;
            if(transferListener != null){
                transferListener.onProgressChanged(count, total);
            }
        }
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
            Log.d(TAG, "Stored File not found: " + createKey(prefix, name));
            errors.put(id, new QblServerException(404, "File not found"));
        } else {
            try {
                copyWithProgress(storedFile, file, boxTransferListener);
            } catch (IOException e) {
                errors.put(id, new QblServerException(400, "Fake transfer manager"));
                return id;
            }
            if (boxTransferListener != null) {
                boxTransferListener.onFinished();
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
        Log.d(TAG, "Delete File: " + createKey(prefix, name));
        if (remove != null) {
            remove.delete();
        }
        return 0;
    }
}
