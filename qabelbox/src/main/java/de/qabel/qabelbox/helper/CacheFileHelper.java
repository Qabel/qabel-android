
package de.qabel.qabelbox.helper;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;

/**
 * Created by danny on 15.02.2016.
 */
public class CacheFileHelper {

    private final String TAG = this.getClass().getSimpleName();

    public void freeCacheAsynchron(Context context) {


        Log.d(TAG, "clear cache");
            clearCache(context);

    }

    void clearCache(final Context context) {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                deleteFiles(context);
                Log.d(TAG, "cache cleared");
                return null;
            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    public void deleteFiles(Context context) {

        File cacheDir = context.getCacheDir();

        File[] files = cacheDir.listFiles();
        long fileSizes = 0;
        for (File file : files) {
            fileSizes += file.length();
        }

        //clear all upload files
        long deletedSize = 0;
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("upload")) {
                    //delete all upload files
                    Log.v(TAG, "delete upload file " + file.getName() + " " + file.length());
                    deletedSize += file.length();
                    file.delete();
                } else {
                    if (file.getName().startsWith("download") && file.length() > 1024 * 1024) {
                        Log.v(TAG, "delete download file " + file.getName() + " " + file.length());
                        deletedSize += file.length();
                        file.delete();
                    }
                    else
                    {
                        if (file.getName().startsWith("dir"))
                        {
                            Log.v(TAG, "delete dir file " + file.getName() + " " + file.length());
                            deletedSize += file.length();
                            file.delete();
                        }
                    }
                }
            }
        }
        Log.d(TAG, "cache cleared before: " + fileSizes / 1024 + "kb, removed: " + deletedSize / 1024 + "kb");
    }
}

