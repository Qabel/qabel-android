package de.qabel.qabelbox.config;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Serializable;

import de.qabel.core.config.Persistable;

/**
 * Created by danny on 27.01.16.
 */
public class AppPreference {

    private final String DB_NAME = "qabel_settings";
    private final int DB_VERSION = 1;
    private final String TAG = this.getClass().getSimpleName();
    private Context mContext;
    private AndroidPersistence androidPersistence;
    private final SharedPreferences settings;
    private final String P_TOKEN = "token";
    private final String P_LAST_APP_START_VERSION = "lastappstartversion";
    private final String P_LAST_APP_UPDATE_QUESTION_TIME = "lastupdatequestiontime";
    private final long NEW_UPATE_QUESTION_TIME_INTERVAL = 1000 * 60 * 60 * 24 * 3l;

    public AppPreference(Context context) {

        settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        /*
        QblSQLiteParams params = new QblSQLiteParams(context, DB_NAME, null, DB_VERSION);
        try {
            androidPersistence = new AndroidPersistence(params);
        } catch (
                QblInvalidEncryptionKeyException e
                ) {
            Log.e(TAG, "error on create app preference database", e);
        }
        */
    }

    public void setToken(String token) {

        settings.edit().putString(P_TOKEN, encrypt(token)).commit();
        /*
        StringEntity values = (StringEntity) androidPersistence.getEntities(StringEntity.class);
        if (values == null) {
            values = new StringEntity(token);
        } else {
            values.token = token;
        }

        androidPersistence.updateOrPersistEntity(values);*/
    }

    private String encrypt(String data) {
        //@todo add functions to encrypt
        return data;
    }

    private String decrypt(String data) {
        //@todo add functions to encrypt
        return data;
    }

    public String getToken() {

        String token = settings.getString(P_TOKEN, null);
        return (token == null) ? null : decrypt(token);

        /*StringEntity values = ((StringEntity) androidPersistence.getEntities(StringEntity.class));
        return (values == null || values.token == null || values.token.length() == 0) ? null : values.token;*/
    }

    public int getLastAppStartVersion() {

        return settings.getInt(P_LAST_APP_START_VERSION, 0);
    }

    public void setLastAppStartVersion(int version) {

        settings.edit().putInt(P_LAST_APP_START_VERSION, version).commit();
    }

    private long getLastAppUpdateQuestion() {

        return settings.getLong(P_LAST_APP_UPDATE_QUESTION_TIME, 0);
    }

    public void setLastAppUpdateQuestion(long time) {

        settings.edit().putLong(P_LAST_APP_UPDATE_QUESTION_TIME, time).commit();
    }

    public boolean shouldUpdateQuestionShowed(long currentTime) {

        return getLastAppUpdateQuestion() + NEW_UPATE_QUESTION_TIME_INTERVAL < currentTime;
    }

    static public class StringEntity extends Persistable implements Serializable {

        private static final long serialVersionUID = 6795130118505894235L;
        public final String token;

        public StringEntity(String token) {

            this.token = token;
        }

        @Override
        public boolean equals(Object o) {

            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            StringEntity that = (StringEntity) o;

            return !(token != null ? !token.equals(that.token) : that.token != null);
        }

        @Override
        public int hashCode() {

            return token != null ? token.hashCode() : 0;
        }
    }
}
