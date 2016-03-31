
package de.qabel.qabelbox.helper;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by danny on 15.02.2016.
 * <p>
 * class to clear cache after app is closed.
 */
public class CacheFileHelper {

    private final String TAG = this.getClass().getSimpleName();
    private static AtomicBoolean isRunningTest;
    private final String[] fileDeletePrefixes = new String[]{"download", "dir", "uploadAnd"};

    public void freeCacheAsynchron(Context context) {


        Log.d(TAG, "clear cache");
        if (!isRunningTest()) {
            clearCache(context);
        } else {
            Log.d(TAG, "skip clear cache. test is rumning");
        }
    }

    private void clearCache(final Context context) {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                deleteFiles(context);
                return null;
            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    private void deleteFiles(Context context) {

        File cacheDir = context.getCacheDir();
        File[] files = cacheDir.listFiles();

        //calculate file size before delete any files
        long fileSizes = 0;
        for (File file : files) {
            fileSizes += file.length();
        }

        //clear files
        long deletedSize = 0;
        for (File file : files) {
            if (shouldFileDeleted(file.getName())) {
                Log.v(TAG, "delete file " + file.getName() + " " + file.length());
                deletedSize += file.length();
                if (!file.delete()) {
                    Log.w(TAG, "error on remove file " + file.getName());
                }
            }
        }

        //show result
        Log.d(TAG, "cache cleared done. before: " + fileSizes / 1024 + "kb, removed: " + deletedSize / 1024 + "kb");
    }

    private boolean shouldFileDeleted(String name) {
        for (String prefix : fileDeletePrefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static synchronized boolean isRunningTest() {
        if (null == isRunningTest) {
            boolean isTest;

            try {
                Class.forName("android.support.test.espresso.Espresso");
                isTest = true;
            } catch (ClassNotFoundException e) {
                isTest = false;
            }

            isRunningTest = new AtomicBoolean(isTest);
        }

        return isRunningTest.get();
    }
}

