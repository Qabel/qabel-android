package de.qabel.qabelbox.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class QblSQLiteOpenHelper extends SQLiteOpenHelper {

    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS CONFIG (ID TEXT PRIMARY KEY NOT NULL, DATA TEXT NOT NULL)";

    public QblSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
       sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
