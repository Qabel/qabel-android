
package de.qabel.qabelbox.helper;

import android.app.ActivityManager;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.List;

import de.qabel.qabelbox.activities.MainActivity;

/**
 * Created by danny on 15.02.2016.
 */
public class CacheFileHelper {

    private final String TAG = this.getClass().getSimpleName();

    public void freeCacheAsynchron(Context context) {

        if (needClear(context)) {
            Log.d(TAG, "clear cache");
            clearCache(context);
        } else {
            Log.d(TAG, "clear cache not needed");
        }
    }

    /**
     * check if cache needed to clear.
     * <p/>
     * the clear cache is needed if no one other activity in the foreground from qabel app
     *
     * @param context
     * @return
     */
    private boolean needClear(Context context) {

        ActivityManager mngr = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningTaskInfo> taskList = mngr.getRunningTasks(10);

        ActivityManager.RunningTaskInfo activity = taskList.get(0);
        if (activity.topActivity.getClassName().equals(MainActivity.class.getName())) {
            if (activity.numActivities == 1) {
                Log.i(TAG, "This is last activity in the stack " + activity.topActivity.getClassName());
                return true;
            } else {
                Log.i(TAG, "More than one activitys active: " + activity.numActivities + " " + activity.numRunning + " " + activity.topActivity.getClassName());
                return false;
            }
        } else {
            Log.i(TAG, "Other activity in foreground." + activity.topActivity.getClassName());
            return true;
        }
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

