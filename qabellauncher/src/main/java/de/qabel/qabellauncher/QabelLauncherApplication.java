package de.qabel.qabellauncher;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

import de.qabel.ackack.event.EventEmitter;
import de.qabel.core.config.ResourceActor;
import de.qabel.core.exceptions.QblInvalidEncryptionKeyException;
import de.qabel.qabellauncher.config.AndroidPersistence;
import de.qabel.qabellauncher.config.QblSQLiteParams;

/**
 * Launches the database password entry dialog, initialized AndroidPersistence and ResourceActor
 * and notifies via a LocalBroadcastManager when these resources are ready.
 */
public class QabelLauncherApplication extends Application {

    public static final String RESOURCES_INITIALIZED = "ResourcesInitialized";

    private static final String DB_NAME = "qabel-service";
    private static final int DB_VERSION = 1;
    private static final String LOG_TAG_QABEL_SERVICE_APP = "Qabel-Service-App";
    private static final String DATABASE_PASSWORD_SET = "DatabasePasswordSet";

    private static ResourceActor resourceActor;
    private static boolean resourceActorInitialized;

    private Thread resourceActorThread;

    static {
        // Enforce SpongyCastle as JCE provider
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(DATABASE_PASSWORD_SET, false)) {
            launchEnterPasswordAction(false);
        } else {
            launchEnterPasswordAction(true);
        }
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
