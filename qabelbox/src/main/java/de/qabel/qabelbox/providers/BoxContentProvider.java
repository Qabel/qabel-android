package de.qabel.qabelbox.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

public class BoxContentProvider extends ContentProvider {

    public static final String PREFIX_CONTENT = "content://";
    public static final String ROW_ID = "ID";
    public static final String ROW_TYPE = "TYPE";
    public static final String ROW_NAME = "NAME";
    public static final String ROW_PARENT = "PARENT";
    public static final String SUFFIX_FOLDER = "/folder";
    public static final String SUFFIX_FILE = "/file";
    public static final String TYPE_FILE = "FILE";
    public static final String TYPE_FOLDER = "FOLDER";
    public static final String AUTHORITY = "de.qabel.qabelbox.providers.BoxContentProvider";
    public static final String ACCOUNT_TYPE = "de.qabel";

    private static final String DB_NAME = "BoxContent";
    private static UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final String CONTENT_AUTHORITY = "de.qabel.qabelbox.providers.BoxContentProvider";

    private static final int MATCH_ROOT = 0;
    private static final int MATCH_FOLDER = 1;
    private static final int MATCH_FILE = 2;

    private BoxSQLiteOpenHelper boxSQLiteOpenHelper;

    static {
        uriMatcher.addURI(CONTENT_AUTHORITY, "", MATCH_ROOT);
        uriMatcher.addURI(CONTENT_AUTHORITY, "folder", MATCH_FOLDER);
        uriMatcher.addURI(CONTENT_AUTHORITY, "file", MATCH_FILE);
    }

    @Override
    public boolean onCreate() {
        boxSQLiteOpenHelper = new BoxSQLiteOpenHelper(getContext(), DB_NAME, null, 1);
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = boxSQLiteOpenHelper.getWritableDatabase();
        switch (uriMatcher.match(uri)) {
            case MATCH_ROOT:
                break;
            case MATCH_FILE:
                break;
            case MATCH_FOLDER:
                return database.query("BoxFileSystem", projection, selection, selectionArgs, null, null, sortOrder);
            default:
                break;
        }
        return null;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case MATCH_ROOT:
                return TYPE_FOLDER;
            case MATCH_FILE:
                return TYPE_FILE;
            case MATCH_FOLDER:
                return TYPE_FOLDER;
        }
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase database = boxSQLiteOpenHelper.getWritableDatabase();
        Uri newUri;
        database.beginTransaction();
        switch (uriMatcher.match(uri)) {
            case MATCH_ROOT:
                break;
            case MATCH_FILE:
                database.insert("BoxFileSystem", null, values);
                database.setTransactionSuccessful();
                break;
            case MATCH_FOLDER:
                break;
            default:
                break;
        }
        database.endTransaction();
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        switch (uriMatcher.match(uri)) {
            case MATCH_ROOT:
                break;
            case MATCH_FILE:
                break;
            case MATCH_FOLDER:
                break;
            default:
                break;
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (uriMatcher.match(uri)) {
            case MATCH_ROOT:
                break;
            case MATCH_FILE:
                break;
            case MATCH_FOLDER:
                break;
            default:
                break;
        }
        return 0;
    }
}
