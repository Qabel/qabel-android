package de.qabel.qabelbox.chat;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import de.qabel.core.config.Identity;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.communication.model.ChatMessageItem;

/**
 * class to store chat messages in database
 */
public class ChatMessagesDataBase extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "ChatMessages.db";
    private static final String TAG = "ChatMessagesDataBase";

    //table for store last load
    private static final String TABLE_NAME_LOAD = "load";
    private static final String COL_LOAD_TIMESTAMP = "timestamp";

    private static final String TABLE_MESSAGE_NAME = "messages";
    private static final String COL_MESSAGE_ID = "id";
    private static final String COL_MESSAGE_ISNEW = "isnew";
    private static final String COL_MESSAGE_TIMESTAMP = "timestamp";
    private static final String COL_MESSAGE_SENDER = "sender";
    private static final String COL_MESSAGE_RECEIVER = "receiver";
    private static final String COL_MESSAGE_ACKNOWLEDGE_ID = "ackid";
    private static final String COL_MESSAGE_PAYLOAD_TYPE = "payload_type";
    private static final String COL_MESSAGE_PAYLOAD = "payload";

    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGE_NAME + " (" +
                    COL_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    COL_MESSAGE_SENDER + " TEXT," +
                    COL_MESSAGE_RECEIVER + " TEXT," +
                    COL_MESSAGE_ACKNOWLEDGE_ID + " TEXT," +
                    COL_MESSAGE_TIMESTAMP + " LONG NOT NULL," +
                    COL_MESSAGE_PAYLOAD_TYPE + " TEXT NOT NULL," +
                    COL_MESSAGE_ISNEW + " INTEGER," +
                    COL_MESSAGE_PAYLOAD + " TEXT);";

    private static final String CREATE_TABLE_LOAD =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_LOAD + " (" +

                    COL_LOAD_TIMESTAMP + " LONG NOT NULL);";
    private final String fullDBName;
    private final Identity identity;

    public ChatMessagesDataBase(QabelBoxApplication context, Identity activeIdentity) {

        super(context, DATABASE_NAME + activeIdentity.getEcPublicKey().getReadableKeyIdentifier(), null, DATABASE_VERSION);
        fullDBName = DATABASE_NAME + activeIdentity.getEcPublicKey().getReadableKeyIdentifier();
        Log.d(TAG, "fulldbname: " + fullDBName);
        this.identity = activeIdentity;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        Log.v(TAG, CREATE_TABLE);
        sqLiteDatabase.execSQL(CREATE_TABLE);
        sqLiteDatabase.execSQL(CREATE_TABLE_LOAD);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int from, int to) {

        sqLiteDatabase.execSQL("DROP TABLE " + TABLE_MESSAGE_NAME + ";");
        sqLiteDatabase.execSQL("DROP TABLE " + CREATE_TABLE_LOAD + ";");
    }

    private void remove(ChatMessageDatabaseItem item) {

        SQLiteDatabase database = getReadableDatabase();
        int rows = database.delete(TABLE_MESSAGE_NAME, COL_MESSAGE_ID + "=?",
                new String[]{"" + item.id});
        Log.i(TAG, "entrys deleted with id: " + item.id + " count: " + rows);
    }

    public long put(ChatMessageDatabaseItem item) {

        remove(item);
        Log.i(TAG, "Put into db: " + item.toString() + " " + item.getSenderKey() + " " + item.getReceiverKey());
        ContentValues values = new ContentValues();

        values.put(COL_MESSAGE_SENDER, item.getSenderKey());
        values.put(COL_MESSAGE_RECEIVER, item.getReceiverKey());
        values.put(COL_MESSAGE_TIMESTAMP, item.time_stamp);
        values.put(COL_MESSAGE_PAYLOAD_TYPE, item.drop_payload_type);
        values.put(COL_MESSAGE_PAYLOAD, item.drop_payload == null ? "" : item.drop_payload);
        values.put(COL_MESSAGE_ISNEW, item.isNew);

        long id = getWritableDatabase().insert(TABLE_MESSAGE_NAME, null, values);
        if (id == -1) {
            Log.e(TAG, "Failed putting into db: " + item.toString());
        } else {
            Log.v(TAG, "db entry putted " + COL_MESSAGE_PAYLOAD);
        }
        return id;
    }

    public ChatMessageDatabaseItem[] get(String key) {

        SQLiteDatabase database = getReadableDatabase();

        Cursor cursor = database.query(TABLE_MESSAGE_NAME, getAllColumnsList(),
                //selection
                COL_MESSAGE_SENDER + "=? OR " +
                        COL_MESSAGE_RECEIVER + "=",
                //selection args
                new String[]{key, key},
                null, null, null);

        return createResultList(cursor);
    }

    @NonNull
    private String[] getAllColumnsList() {

        return new String[]
                {
                        //colums
                        COL_MESSAGE_ID,
                        COL_MESSAGE_ISNEW,
                        COL_LOAD_TIMESTAMP,
                        COL_MESSAGE_SENDER,
                        COL_MESSAGE_RECEIVER,
                        COL_MESSAGE_ACKNOWLEDGE_ID,
                        COL_MESSAGE_PAYLOAD_TYPE,
                        COL_MESSAGE_PAYLOAD};
    }

    public ChatMessageDatabaseItem[] getAll() {

        SQLiteDatabase database = getReadableDatabase();

        Cursor cursor = database.query(TABLE_MESSAGE_NAME, getAllColumnsList(),
                //selection
                null, null,
                null, null, null);

        return createResultList(cursor);
    }

    @Nullable
    private ChatMessageDatabaseItem[] createResultList(Cursor cursor) {

        try {
            cursor.moveToFirst();

            int count = cursor.getCount();
            Log.v(TAG, "database result count");
            ChatMessageDatabaseItem[] items = new ChatMessageDatabaseItem[count];
            int i = 0;

            while (!cursor.isAfterLast()) {
                //should match all columns list
                items[i] = new ChatMessageDatabaseItem();
                items[i].id = cursor.getInt(0);
                items[i].isNew = cursor.getShort(1);
                items[i].time_stamp = cursor.getLong(2);
                items[i].sender = cursor.getString(3);
                items[i].receiver = cursor.getString(4);
                items[i].acknowledge_id = cursor.getString(5);
                items[i].drop_payload_type = cursor.getString(6);
                items[i].drop_payload = cursor.getString(7);
                i++;
                cursor.moveToNext();
            }

            cursor.close();
            return items;
        } catch (Exception e) {
            cursor.close();
            Log.e(TAG, "error on db access", e);
            return null;
        }
    }

    public static class ChatMessageDatabaseItem extends ChatMessageItem {

        public short isNew = 1;//1 for new, 0 for old
        public int id;
    }
}
