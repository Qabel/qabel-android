package de.qabel.qabelbox.config;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.params.KeyParameter;

import java.util.ArrayList;
import java.util.List;

import de.qabel.core.config.Persistable;
import de.qabel.core.config.Persistence;
import de.qabel.core.exceptions.QblInvalidEncryptionKeyException;

public class AndroidPersistence extends Persistence<QblSQLiteParams> {

    private static final String STR_SALT = "SALT";
    private static final String STR_CONFIG_TABLE = "CONFIG";
    private static final String STR_MASTER_KEY = "MASTERKEY";
    private static final String STR_MASTER_KEY_NONCE = "MASTERKEYNONCE";
    private static final String STR_DATA = "DATA";
    private static final String STR_NONCE = "NONCE";
    private static final String STR_BLOB = "BLOB";
    private static final String STR_ID = "ID";
    private static final String STR_ID_QUERY = "ID = ?";
    private QblSQLiteOpenHelper dbHelper;
    private SQLiteDatabase database;

    private static final Logger LOGGER = LoggerFactory.getLogger(AndroidPersistence.class.getName());

    public AndroidPersistence(QblSQLiteParams params, char[] password) throws QblInvalidEncryptionKeyException {
        super(params, password);
    }

    @Override
    public boolean connect(QblSQLiteParams params) {
        dbHelper = new QblSQLiteOpenHelper(params.getContext(), params.getName(),
                params.getFactory(), params.getVersion());
        database = dbHelper.getWritableDatabase();
        return true;
    }

    @Override
    protected byte[] getSalt(boolean forceNewSalt) {
        byte[] salt = null;

        if (forceNewSalt) {
            deleteConfigValue(STR_SALT);
        } else {
            salt = getConfigValue(STR_SALT);
        }

        if (salt == null) {
            salt = cryptoutils.getRandomBytes(SALT_SIZE_BYTE);
            setConfigValue(STR_SALT, salt);
        }
        return salt;
    }

    private byte[] getConfigValue(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty!");
        }
        String[] selectionArgs = { name };

        Cursor cursor = null;
        byte[] value = null;
        try {
            String[] columns = new String[]{STR_DATA};
            cursor = database.query(STR_CONFIG_TABLE, columns, STR_ID_QUERY, selectionArgs,
                    null, null, null);

            if (cursor.moveToFirst()) {
                value = cursor.getBlob(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return value;
    }

    private boolean setConfigValue(String name, byte[] data) {
        if (name == null || data == null) {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty!");
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(STR_ID, name);
        contentValues.put(STR_DATA, data);
        return database.insert(STR_CONFIG_TABLE, null, contentValues) != -1L;
    }

    private boolean deleteConfigValue(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty!");
        }
        String[] selectionArgs = { name };
        return database.delete(STR_CONFIG_TABLE, STR_ID_QUERY, selectionArgs) != 0;
    }

    private byte[] getNonce(String id, Class cls) {
        if (id == null || cls == null) {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }
        if (id.isEmpty()) {
            throw new IllegalArgumentException("ID cannot be empty!");
        }
        String[] columns = {STR_NONCE};
        String[] selectionArgs = { id };

        Cursor cursor = null;
        byte[] value = null;
        try {
            cursor = database.query(getTableNameForClass(cls), columns, STR_ID_QUERY, selectionArgs,
                    null, null, null);

            if (cursor.moveToFirst()) {
                value = cursor.getBlob(0);
            } else {
                LOGGER.info("Cannot select NONCE for " + id + '!');
            }
        }
        catch (SQLiteException e) {
            LOGGER.info("Cannot select NONCE for " + id + '!');
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return value;
    }

    @Override
    protected KeyParameter getMasterKey(KeyParameter encryptionKey) {
        if (encryptionKey == null) {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }
        byte[] masterKey = null;
        byte[] masterKeyNonce = getConfigValue(STR_MASTER_KEY_NONCE);

        if (masterKeyNonce != null) {
            try {
                masterKey = cryptoutils.decrypt(encryptionKey, masterKeyNonce, getConfigValue(STR_MASTER_KEY), null);
            } catch (InvalidCipherTextException e) {
                LOGGER.error("Cannot decrypt master key!", e);
                return null;
            }
        }

        if (masterKey == null) {
            masterKey = cryptoutils.getRandomBytes(AES_KEY_SIZE_BYTE);
            masterKeyNonce = cryptoutils.getRandomBytes(NONCE_SIZE_BYTE);

            if (!setConfigValue(STR_MASTER_KEY_NONCE, masterKeyNonce)) {
                LOGGER.error("Cannot insert master key nonce into database!");
                return null;
            }

            try {
                if (!setConfigValue(STR_MASTER_KEY, cryptoutils.encrypt(encryptionKey, masterKeyNonce, masterKey, null))) {
                    LOGGER.error("Cannot insert master key into database!");
                    return null;
                }
            } catch (InvalidCipherTextException e) {
                LOGGER.error("Cannot encrypt master key!", e);
                return null;
            }
        }
        return new KeyParameter(masterKey);
    }

    @Override
    public boolean reEncryptMasterKey(KeyParameter oldKey, KeyParameter newKey) {
        if (oldKey == null || newKey == null) {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }
        KeyParameter oldMasterKey = getMasterKey(oldKey);
        if (oldMasterKey == null) {
            LOGGER.error("Cannot decrypt master key. Wrong password?");
            return false;
        }
        boolean success = false;
        try {
            database.beginTransaction();

            deleteConfigValue(STR_MASTER_KEY);
            deleteConfigValue(STR_MASTER_KEY_NONCE);

            byte[] masterKeyNonce = cryptoutils.getRandomBytes(NONCE_SIZE_BYTE);
            setConfigValue(STR_MASTER_KEY_NONCE, masterKeyNonce);
            setConfigValue(STR_MASTER_KEY, cryptoutils.encrypt(newKey, masterKeyNonce, oldMasterKey.getKey(), null));
            database.setTransactionSuccessful();
            success = true;
        } catch (InvalidCipherTextException e) {
            LOGGER.error("Cannot re-encrypt master key!", e);
        }
        finally {
            database.endTransaction();
        }
        return success;
    }

    @Override
    public boolean persistEntity(Persistable object) {
        if (object == null) {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }

        String sql = "CREATE TABLE IF NOT EXISTS " +
                getTableNameForClass(object.getClass()) +
                "(ID TEXT PRIMARY KEY NOT NULL," +
                "NONCE TEXT NOT NULL," +
                "BLOB BLOB NOT NULL)";

        try {
            database.execSQL(sql);
        } catch (SQLException e) {
            LOGGER.error("Cannot create table!", e);
        }

        byte[] nonce = cryptoutils.getRandomBytes(NONCE_SIZE_BYTE);

        ContentValues contentValues = new ContentValues();
        contentValues.put(STR_ID, object.getPersistenceID());
        contentValues.put(STR_NONCE, nonce);
        contentValues.put(STR_BLOB, serialize(object.getPersistenceID(), object, nonce));

        return database.insert(getTableNameForClass(object.getClass()), null, contentValues) != -1L;
    }

    @Override
    public boolean updateEntity(Persistable object) {
        if (object == null) {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }

        byte[] nonce = getNonce(object.getPersistenceID(), object.getClass());

        if (nonce == null) {
            LOGGER.info("Entity not stored!");
            return false;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(STR_NONCE, nonce);
        contentValues.put(STR_BLOB, serialize(object.getPersistenceID(), object, nonce));

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
    public Persistable getEntity(String id, Class cls) {
        if (id == null || cls == null) {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }
        if (id.isEmpty()) {
            throw new IllegalArgumentException("ID cannot be empty!");
        }

        String[] columns = {STR_BLOB, STR_NONCE};
        String[] selectionArgs = { id };

        Cursor cursor = null;
        try {
            cursor = database.query(getTableNameForClass(cls), columns, STR_ID_QUERY,
                    selectionArgs, null, null, null);

            if (cursor.moveToFirst()) {
                return (Persistable) deserialize(id, cursor.getBlob(0), cursor.getBlob(1));
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
    public List<Persistable> getEntities(Class cls) {
        if (cls == null) {
            throw new IllegalArgumentException("Arguments cannot be null!");
        }
        List<Persistable> objects = new ArrayList<>();

        String[] columns = {STR_ID, STR_BLOB, STR_NONCE};

        Cursor cursor = null;
        try {
            cursor = database.query(getTableNameForClass(cls), columns,
                    null, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    objects.add((Persistable) deserialize(cursor.getString(0),
                            cursor.getBlob(1), cursor.getBlob(2)));
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
