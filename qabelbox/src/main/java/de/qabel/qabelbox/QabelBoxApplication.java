package de.qabel.qabelbox;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.util.encoders.Hex;

import java.security.Security;

import de.qabel.ackack.event.EventEmitter;
import de.qabel.core.config.ResourceActor;
import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.exceptions.QblInvalidEncryptionKeyException;
import de.qabel.qabelbox.activities.MainActivity;
import de.qabel.qabelbox.config.AndroidPersistence;
import de.qabel.qabelbox.config.QblSQLiteParams;
import de.qabel.qabelbox.providers.BoxProvider;

public class QabelBoxApplication extends Application {
    public static final String RESOURCES_INITIALIZED = "ResourcesInitialized";
    public static final String DEFAULT_DROP_SERVER = "http://localhost";

    private static final String DB_NAME = "qabel-service";
    private static final int DB_VERSION = 1;
    private static final String LOG_TAG_QABEL_SERVICE_APP = "Qabel-Service-App";
    private static final String DATABASE_PASSWORD_SET = "DatabasePasswordSet";
    private static final String PREF_DEVICE_ID_CREATED = "PREF_DEVICE_ID_CREATED";
    private static final String PREF_DEVICE_ID = "PREF_DEVICE_ID";
    private static final int NUM_BYTES_DEVICE_ID = 16;
    private static final String PREF_LAST_ACTIVE_IDENTITY = "PREF_LAST_ACTIVE_IDENTITY";

    public static BoxProvider boxProvider;

    private static SharedPreferences sharedPreferences;
    private static ResourceActor resourceActor;
    private static boolean resourceActorInitialized;

    private Thread resourceActorThread;

    static {
        // Enforce SpongyCastle as JCE provider
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    public static ResourceActor getResourceActor() {
        if (resourceActor == null) {
            throw new RuntimeException("ResourceActor has not been initialized yet. " +
                    "Subscribe to QabelAndroidApplication.RESOURCES_INITIALIZED broadcast to " +
                    "be notified when ResourceActor is initialized.");
        }
        return resourceActor;
    }

    public static boolean isResourceActorInitialized() {
        return resourceActorInitialized;
    }

    public static void setLastActiveIdentityID(String identityID) {
            sharedPreferences.edit()
                    .putString(PREF_LAST_ACTIVE_IDENTITY, identityID)
                    .apply();
    }

    public static String getLastActiveIdentityID() {
        return sharedPreferences.getString(PREF_LAST_ACTIVE_IDENTITY, "");
    }

    public BoxProvider getProvider() {
        return boxProvider;
    }

    public static byte[] getDeviceID() {
        String deviceID = sharedPreferences.getString(PREF_DEVICE_ID, "");
        if (deviceID.equals("")) {
            // Should never occur
            throw new RuntimeException("DeviceID not created!");
        }
        return Hex.decode(deviceID);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences(this.getClass().getCanonicalName(), MODE_PRIVATE);

        if (!sharedPreferences.getBoolean(PREF_DEVICE_ID_CREATED, false)) {

            CryptoUtils cryptoUtils = new CryptoUtils();
            byte[] deviceID = cryptoUtils.getRandomBytes(NUM_BYTES_DEVICE_ID);

            Log.d(this.getClass().getName(), "New device ID: " + Hex.toHexString(deviceID));

            sharedPreferences.edit().putString(PREF_DEVICE_ID, Hex.toHexString(deviceID))
                                    .putBoolean(PREF_DEVICE_ID_CREATED, true)
                                    .apply();
        }

        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(DATABASE_PASSWORD_SET, false)) {
            launchEnterPasswordAction(false);
        } else {
            launchEnterPasswordAction(true);
        }
    }

    /**
     * Launches the MainActivity with the OpenDatabaseFragment
     */
    private void launchEnterPasswordAction(boolean newPassword) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (newPassword) {
            intent.setAction(MainActivity.ACTION_ENTER_NEW_DB_PASSWORD);
        }
        else {
            intent.setAction(MainActivity.ACTION_ENTER_DB_PASSWORD);
        }
        startActivity(intent);
    }

    /**
     * Initializes the global resources for the Application
     * @param password Database decryption password
     * @return Result of init operation.
     */
    public boolean init(char[] password) {
        AndroidPersistence androidPersistence;
        QblSQLiteParams params = new QblSQLiteParams(this, DB_NAME, null, DB_VERSION);
        try {
            androidPersistence = new AndroidPersistence(params, password);
        } catch (QblInvalidEncryptionKeyException e) {
            Log.e(LOG_TAG_QABEL_SERVICE_APP, "Invalid database password!");
            return false;
        }

        // Save DATABASE_PASSWORD_SET in SharedPreferences
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean(DATABASE_PASSWORD_SET, true);
        editor.apply();

        resourceActor = new ResourceActor(androidPersistence, EventEmitter.getDefault());
        resourceActorThread = new Thread(resourceActor, "ResourceActorThread");
        resourceActorThread.start();

        resourceActorInitialized = true;

        // Send resources initialized local broadcast
        Intent intent = new Intent();
        intent.setAction(RESOURCES_INITIALIZED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        return true;
    }
}
