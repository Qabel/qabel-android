package de.qabel.qabelbox.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;

import de.qabel.qabelbox.storage.FileCacheContract.FileEntry;

class FileCache extends SQLiteOpenHelper {

    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + FileEntry.TABLE_NAME + "( " +
                    FileEntry.COL_REF + " TEXT NOT NULL," +
                    FileEntry.COL_PATH + " TEXT NOT NULL," +
                    FileEntry.COL_MTIME + " LONG NOT NULL," +
                    FileEntry.COL_SIZE + " LONG NOT NULL);";

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "FileCache.db";
    private static final String TAG = "FileCache";

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

    public void remove(BoxFile boxFile) {
        SQLiteDatabase database = getReadableDatabase();
        int rows = database.delete(FileEntry.TABLE_NAME, FileEntry.COL_REF + "=?",
                new String[]{boxFile.block});
        if (rows == 0) {
            Log.i(TAG, "Trying to remove non existing cache entry: " + boxFile.block);
        }
    }

    public long put(BoxFile boxFile, File file) {
        remove(boxFile);
        Log.i(TAG, "Put into cache: " + boxFile.block);
        ContentValues values = new ContentValues();
        values.put(FileEntry.COL_REF, boxFile.block);
        values.put(FileEntry.COL_PATH, file.getAbsolutePath());
        values.put(FileEntry.COL_MTIME, boxFile.mtime);
        values.put(FileEntry.COL_SIZE, boxFile.size);
        long id = getWritableDatabase().insert(FileEntry.TABLE_NAME, null, values);
        if (id == -1) {
            Log.e(TAG, "Failed putting into cache: " + boxFile.block);
        }
        return id;
    }

    public File get(BoxFile boxFile) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(FileEntry.TABLE_NAME, new String[]{FileEntry.COL_PATH},
                FileEntry.COL_REF + "=? AND " +
                        FileEntry.COL_MTIME + "=" + boxFile.mtime.toString() + " AND " +
                        FileEntry.COL_SIZE + "=" + boxFile.size.toString(),
                new String[]{boxFile.block}, null, null, null);
        cursor.moveToFirst();
        try {
            String path = cursor.getString(cursor.getColumnIndexOrThrow(FileEntry.COL_PATH));
            cursor.close();
            File file = new File(path);
            if (file.exists()) {
                return file;
            } else {
                remove(boxFile);
                return null;
            }
        } catch (CursorIndexOutOfBoundsException e) {
            cursor.close();
            return null;
        }
    }
}
