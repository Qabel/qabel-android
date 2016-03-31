package de.qabel.qabelbox.persistence;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import de.qabel.core.config.Persistable;
import de.qabel.qabelbox.exceptions.QblPersistenceException;

import java.util.ArrayList;
import java.util.List;

public class AndroidDatabaseWrapper extends DatabaseWrapperImpl<QblSQLiteParams> {

    private QblSQLiteOpenHelper dbHelper;
    private SQLiteDatabase database;
    private QblSQLiteParams params;

    public AndroidDatabaseWrapper(QblSQLiteParams params) {
        this.params = params;
    }

    @Override
    public boolean connect() {
        dbHelper = new QblSQLiteOpenHelper(params.getContext(), params.getName(),
            params.getFactory(), params.getVersion());
        database = dbHelper.getWritableDatabase();
        return true;
    }

    @Override
    public void execSQL(String sql) throws QblPersistenceException {
        try {
            database.execSQL(sql);
        } catch (SQLException e) {
            throw new QblPersistenceException(e);
        }
    }

    @Override
    public boolean insert(Persistable entity) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(STR_ID, entity.getPersistenceID());
        contentValues.put(STR_BLOB, PersistenceUtil.serialize(entity.getPersistenceID(), entity));

        return database.insert(PersistenceUtil.getTableNameForClass(entity.getClass()), null, contentValues) != -1L;
    }

    @Override
    public boolean update(Persistable entity) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(STR_BLOB, PersistenceUtil.serialize(entity.getPersistenceID(), entity));

        String[] whereArgs = {entity.getPersistenceID()};
        return database.update(PersistenceUtil.getTableNameForClass(entity.getClass()), contentValues, STR_ID_QUERY, whereArgs) != -1;
    }

    @Override
    public boolean delete(String id, Class cls) {
        String[] whereArgs = {id};
        return database.delete(PersistenceUtil.getTableNameForClass(cls), STR_ID_QUERY, whereArgs) == 1;
    }

    @Override
    public <U extends Persistable> U getEntity(String id, Class<? extends U> cls) {
        String[] columns = {STR_BLOB};
        String[] selectionArgs = {id};

        Cursor cursor = null;
        try {
            cursor = database.query(PersistenceUtil.getTableNameForClass(cls), columns, STR_ID_QUERY,
                selectionArgs, null, null, null);

            if (cursor.moveToFirst()) {
                return (U) PersistenceUtil.deserialize(id, cursor.getBlob(0));
            }
        } catch (SQLiteException e) {
            throw new QblPersistenceException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    @Override
    public <U extends Persistable> List<U> getEntities(Class<? extends U> cls) throws QblPersistenceException {
        List<U> objects = new ArrayList<>();

        String[] columns = {STR_ID, STR_BLOB};

        Cursor cursor = null;
        try {
            cursor = database.query(PersistenceUtil.getTableNameForClass(cls), columns,
                null, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    objects.add((U) PersistenceUtil.deserialize(cursor.getString(0),
                        cursor.getBlob(1)));
                } while (cursor.moveToNext());
            }
        } catch (SQLiteException e) {
            throw new QblPersistenceException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return objects;
    }
}
