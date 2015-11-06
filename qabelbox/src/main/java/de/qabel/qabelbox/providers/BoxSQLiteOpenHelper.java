package de.qabel.qabelbox.providers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BoxSQLiteOpenHelper extends SQLiteOpenHelper {

    public BoxSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE BoxFileSystem " + '(' +
                "ID INTEGER NOT NULL PRIMARY KEY, " +
                "TYPE TEXT, " +
                "NAME TEXT, " +
                "PARENT INTEGER " +
                ')');
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}

