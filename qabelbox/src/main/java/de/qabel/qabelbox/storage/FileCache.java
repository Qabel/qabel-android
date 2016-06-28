package de.qabel.qabelbox.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;

import de.qabel.box.storage.BoxFile;
import de.qabel.qabelbox.storage.FileCacheContract.FileEntry;

public class FileCache extends SQLiteOpenHelper {

    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + FileEntry.TABLE_NAME + "( " +
                    FileEntry.COL_REF + " TEXT NOT NULL," +
                    FileEntry.COL_PATH + " TEXT NOT NULL," +
                    FileEntry.COL_MTIME + " LONG NOT NULL," +
                    FileEntry.COL_SIZE + " LONG NOT NULL);";

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "FileCache.db";
    private static final String TAG = "FileCache";

    private class CacheEntry {
        String ref;
        long mTime;
        long size;
        String path;
    }

    public FileCache(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int from, int to) {
        sqLiteDatabase.execSQL("DROP TABLE " + FileEntry.TABLE_NAME + ";");
    }

    public void remove(CacheEntry entry) {
        File cachedFile = new File(entry.path);
        if (cachedFile.exists()) {
            if (!cachedFile.delete()) {
                Log.d(TAG, "Cannot delete cached file.");
            }
        }
        SQLiteDatabase database = getWritableDatabase();
        int rows = database.delete(FileEntry.TABLE_NAME, FileEntry.COL_REF + "=?",
                new String[]{entry.ref});
        if (rows == 0) {
            Log.i(TAG, "Trying to remove non existing cache entry: " + entry.ref);
        }
    }

    public void remove(String ref) {
        CacheEntry entry = getCachedEntry(ref);
        if (entry != null) {
            remove(entry);
        }
    }

    public long put(BoxFile boxFile, File file) {
        remove(boxFile.getBlock());
        Log.i(TAG, "Put into cache: " + boxFile.getBlock() + "(" + file.getAbsolutePath() + ")");
        ContentValues values = new ContentValues();
        values.put(FileEntry.COL_REF, boxFile.getBlock());
        values.put(FileEntry.COL_PATH, file.getAbsolutePath());
        values.put(FileEntry.COL_MTIME, boxFile.getMtime());
        values.put(FileEntry.COL_SIZE, file.length());
        long id = getWritableDatabase().insert(FileEntry.TABLE_NAME, null, values);
        if (id == -1) {
            Log.e(TAG, "Failed putting into cache: " + boxFile.getBlock());
        }
        return id;
    }

    public File get(BoxFile boxFile) {
        CacheEntry cacheEntry = getCachedEntry(boxFile.getBlock());

        Log.i(TAG, "get from cache: " + boxFile.getBlock() + "(" + (cacheEntry != null ? cacheEntry.path : "null") + ")");
        if (cacheEntry != null) {
            File file = new File(cacheEntry.path);
            if (boxFile.getMtime() == cacheEntry.mTime &&
                    file.exists() &&
                    file.length() == cacheEntry.size) {
                return file;
            } else {
                remove(cacheEntry);
            }
        }
        return null;
    }

    private CacheEntry getCachedEntry(String ref) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(FileEntry.TABLE_NAME, new String[]{FileEntry.COL_PATH, FileEntry.COL_MTIME, FileEntry.COL_SIZE},
                FileEntry.COL_REF + "=?",
                new String[]{ref}, null, null, null);
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            CacheEntry entry = new CacheEntry();
            entry.ref = ref;
            entry.path = cursor.getString(cursor.getColumnIndex(FileEntry.COL_PATH));
            entry.mTime = cursor.getLong(cursor.getColumnIndex(FileEntry.COL_MTIME));
            entry.size = cursor.getLong(cursor.getColumnIndex(FileEntry.COL_SIZE));
            return entry;
        } catch (CursorIndexOutOfBoundsException e) {
            e.printStackTrace();
            return null;
        } finally {
            cursor.close();
        }
    }
}
