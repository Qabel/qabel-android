package de.qabel.qabelbox.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import de.qabel.qabelbox.communication.model.ChatMessageItem;

/**
 * class to store chat messages in database
 */
public class ChatMessagesDataBase extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "ChatMessages.db";
    private static final String TAG = "ChatMessagesDataBase";

    //table for store last load
    public static final String TABLE_NAME_LOAD = "load";
    public static final String COL_LOAD_TIMESTAMP = "timestamp";

    public static final String TABLE_MESSAGE_NAME = "messages";
    public static final String COL_MESSAGE_ID = "id";
    public static final String COL_MESSAGE_ISNEW = "isnew";
    public static final String COL_MESSAGE_TIMESTAMP = "timestamp";
    public static final String COL_MESSAGE_SENDER = "sender";
    public static final String COL_MESSAGE_RECEIVER = "receiver";
    public static final String COL_MESSAGE_PAYLOAD_TYPE = "payload_type";
    public static final String COL_MESSAGE_PAYLOAD = "payload";

    //@todo dbname muss auch noch identität enthalten
    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGE_NAME + "( " +
                    COL_MESSAGE_ID + " INTEGER AUTOINCREMENT," +
                    COL_MESSAGE_SENDER + " TEXT NOT NULL," +
                    COL_MESSAGE_RECEIVER + " TEXT NOT NULL," +
                    COL_MESSAGE_TIMESTAMP + " LONG NOT NULL," +
                    COL_MESSAGE_PAYLOAD_TYPE + " TEXT NOT NULL," +
                    COL_MESSAGE_ISNEW + " INTEGER NOT NULL," +
                    COL_MESSAGE_PAYLOAD + " TEXT NOT NULL);";

    private static final String CREATE_TABLE_LOAD =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_LOAD + "( " +

                    COL_LOAD_TIMESTAMP + " LONG NOT NULL);";

    public ChatMessagesDataBase(Context context) {

        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        sqLiteDatabase.execSQL(CREATE_TABLE);
        sqLiteDatabase.execSQL(CREATE_TABLE_LOAD);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int from, int to) {

        sqLiteDatabase.execSQL("DROP TABLE " + TABLE_MESSAGE_NAME + ";");
        sqLiteDatabase.execSQL("DROP TABLE " + CREATE_TABLE_LOAD + ";");
    }

    public void remove(ChatMessageDatabaseItem item) {

        SQLiteDatabase database = getReadableDatabase();
        int rows = database.delete(TABLE_MESSAGE_NAME, COL_MESSAGE_ID + "=?",
                new String[]{"" + item.id});
        Log.i(TAG, "entrys deleted with id: " + item.id + " count: " + rows);
    }

    public long put(ChatMessageDatabaseItem item) {

        remove(item);
        Log.i(TAG, "Put into db: " + item.toString());
        ContentValues values = new ContentValues();

        values.put(COL_MESSAGE_SENDER, item.sender);
        values.put(COL_MESSAGE_RECEIVER, item.getReceiverKey());
        values.put(COL_MESSAGE_TIMESTAMP, item.time_stamp);
        values.put(COL_MESSAGE_PAYLOAD_TYPE, item.drop_payload_type);
        values.put(COL_MESSAGE_PAYLOAD, item.drop_payload.toString());
        values.put(COL_MESSAGE_ISNEW, item.isNew);

        long id = getWritableDatabase().insert(TABLE_MESSAGE_NAME, null, values);
        if (id == -1) {
            Log.e(TAG, "Failed putting into db: " + item.toString());
        }
        return id;
    }

    public ChatMessageDatabaseItem[] get(String key) {

        SQLiteDatabase database = getReadableDatabase();

        Cursor cursor = database.query(TABLE_MESSAGE_NAME, new String[]
                        {
                                //colums
                                COL_MESSAGE_ID,
                                COL_MESSAGE_ISNEW,
                                COL_LOAD_TIMESTAMP,
                                COL_MESSAGE_SENDER,
                                COL_MESSAGE_RECEIVER,
                                COL_MESSAGE_PAYLOAD_TYPE,
                                COL_MESSAGE_PAYLOAD},
                //selection
                COL_MESSAGE_SENDER + "=? OR " +
                        COL_MESSAGE_RECEIVER + "=",
                //selection args
                new String[]{key, key},
                null, null, null);

        try {
            cursor.moveToFirst();

            int count = cursor.getCount();
            ChatMessageDatabaseItem[] items = new ChatMessageDatabaseItem[count];
            int i = 0;

            while (!cursor.isAfterLast()) {

                items[i].id = cursor.getInt(0);
                items[i].isNew = cursor.getShort(1);
                items[i].time_stamp = cursor.getLong(2);
                items[i].sender = cursor.getString(3);
                items[i].receiver = cursor.getString(3);
                items[i].drop_payload_type = cursor.getString(4);
                items[i].drop_payload_plain = cursor.getString(5);

                i++;
                cursor.moveToNext();
            }

            cursor.close();
            return items;
        } catch (Exception e) {
            cursor.close();
            return null;
        }
    }

    public static class ChatMessageDatabaseItem extends ChatMessageItem {

        public short isNew = 1;//1 for new, 0 for old
        public int id;
        public String receiver;
        public String drop_payload_plain;
    }
}
