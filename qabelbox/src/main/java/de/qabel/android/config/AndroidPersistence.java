package de.qabel.android.config;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import de.qabel.core.config.Persistable;
import de.qabel.core.config.Persistence;
import de.qabel.core.exceptions.QblInvalidEncryptionKeyException;

public class AndroidPersistence extends Persistence<QblSQLiteParams> {

    private static final String STR_BLOB = "BLOB";
    private static final String STR_ID = "ID";
    private static final String STR_ID_QUERY = "ID = ?";
    private QblSQLiteOpenHelper dbHelper;
    private SQLiteDatabase database;

    private static final Logger LOGGER = LoggerFactory.getLogger(AndroidPersistence.class.getName());

    public AndroidPersistence(QblSQLiteParams params) throws QblInvalidEncryptionKeyException {
        connect(params);
    }

    @Override
    public boolean connect(QblSQLiteParams params) {
        dbHelper = new QblSQLiteOpenHelper(params.getContext(), params.getName(),
                params.getFactory(), params.getVersion());
        database = dbHelper.getWritableDatabase();
        return true;
    }

    @Override
    public boolean persistEntity(Persistable object) {
        if (object == null) {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }

        String sql = "CREATE TABLE IF NOT EXISTS " +
                getTableNameForClass(object.getClass()) +
                "(ID TEXT PRIMARY KEY NOT NULL," +
                "BLOB BLOB NOT NULL)";

        try {
            database.execSQL(sql);
        } catch (SQLException e) {
            LOGGER.error("Cannot create table!", e);
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(STR_ID, object.getPersistenceID());
        contentValues.put(STR_BLOB, serialize(object.getPersistenceID(), object));

        return database.insert(getTableNameForClass(object.getClass()), null, contentValues) != -1L;
    }

    @Override
    public boolean updateEntity(Persistable object) {
        if (object == null) {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }

        if (getEntity(object.getPersistenceID(), object.getClass()) == null) {
            LOGGER.info("Entity not stored!");
            return false;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(STR_BLOB, serialize(object.getPersistenceID(), object));

        String[] whereArgs = { object.getPersistenceID() };
        return database.update(getTableNameForClass(object.getClass()),
                contentValues, STR_ID_QUERY, whereArgs) != -1;
    }

    @Override
    public boolean updateOrPersistEntity(Persistable object) {
        if (getEntity(object.getPersistenceID(), object.getClass()) == null) {
            return persistEntity(object);
        }
        else {
            return updateEntity(object);
        }
    }

    @Override
    public boolean removeEntity(String id, Class cls) {
        if (id == null || cls == null) {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }
        if (id.isEmpty()) {
            throw new IllegalArgumentException("ID cannot be empty!");
        }

        String[] whereArgs = { id };
        return database.delete(getTableNameForClass(cls), STR_ID_QUERY, whereArgs) == 1;
    }

    @Override
    public <U extends Persistable> U  getEntity(String id, Class<? extends U> cls) {
        if (id == null || cls == null) {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }
        if (id.isEmpty()) {
            throw new IllegalArgumentException("ID cannot be empty!");
        }

        String[] columns = {STR_BLOB};
        String[] selectionArgs = { id };

        Cursor cursor = null;
        try {
            cursor = database.query(getTableNameForClass(cls), columns, STR_ID_QUERY,
                    selectionArgs, null, null, null);

            if (cursor.moveToFirst()) {
                return (U) deserialize(id, cursor.getBlob(0));
            }
        } catch (SQLiteException e) {
            LOGGER.debug("Couldn't get entity! " + e.getLocalizedMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    @Override
    public <U extends Persistable> List<U> getEntities(Class<? extends U> cls) {
        if (cls == null) {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }
        List<U> objects = new ArrayList<>();

        String[] columns = {STR_ID, STR_BLOB};

        Cursor cursor = null;
        try {
            cursor = database.query(getTableNameForClass(cls), columns,
                    null, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    objects.add((U) deserialize(cursor.getString(0),
                            cursor.getBlob(1)));
                } while (cursor.moveToNext());
            }
        }
        catch (SQLiteException e){
            LOGGER.info("Table does not exist!");
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return objects;
    }

    @Override
    public boolean dropTable(Class cls) {
        if (cls == null) {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }

        try {
            database.execSQL("DROP TABLE " + getTableNameForClass(cls));
        }
        catch (SQLiteException e) {
            LOGGER.info("Table does not exist!");
            return false;
        }
        return true;
    }

    private static String getTableNameForClass(Class cls) {
        return '\'' + cls.getCanonicalName() + '\'';
    }
}
