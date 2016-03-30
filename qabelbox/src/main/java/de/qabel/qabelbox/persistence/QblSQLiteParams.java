package de.qabel.qabelbox.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

public class QblSQLiteParams {
    private final Context context;
    private final String name;
    private final CursorFactory factory;
    private final int version;

    public QblSQLiteParams(Context context, String name, CursorFactory factory, int version) {
        this.context = context;
        this.name = name;
        this.factory = factory;
        this.version = version;
    }

    public Context getContext() {
        return context;
    }

    public String getName() {
        return name;
    }

    public CursorFactory getFactory() {
        return factory;
    }

    public int getVersion() {
        return version;
    }
}
