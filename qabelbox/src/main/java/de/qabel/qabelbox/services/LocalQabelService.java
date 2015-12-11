package de.qabel.qabelbox.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import de.qabel.ackack.event.EventEmitter;
import de.qabel.core.config.ResourceActor;
import de.qabel.core.exceptions.QblInvalidEncryptionKeyException;
import de.qabel.qabelbox.config.AndroidPersistence;
import de.qabel.qabelbox.config.QblSQLiteParams;

public class LocalQabelService extends Service {

	private static final String TAG = "LocalQabelService";
	private static final char[] PASSWORD = "constantpassword".toCharArray();
	private final IBinder mBinder = new LocalBinder();
	private ResourceActor resourceActor;
	private Thread resourceActorThread;

	private static final String DB_NAME = "qabel-service";
	private static final int DB_VERSION = 1;

	public ResourceActor getResourceActor() {
		return resourceActor;
	}

	public class LocalBinder extends Binder {
		public LocalQabelService getService() {
			// Return this instance of LocalQabelService so clients can call public methods
			return LocalQabelService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		AndroidPersistence androidPersistence;
		QblSQLiteParams params = new QblSQLiteParams(this, DB_NAME, null, DB_VERSION);
		try {
			androidPersistence = new AndroidPersistence(params, PASSWORD);
		} catch (QblInvalidEncryptionKeyException e) {
			Log.e(TAG,  "Invalid database password!");
			return;
		}

		resourceActor = new ResourceActor(androidPersistence, EventEmitter.getDefault());
		resourceActorThread = new Thread(resourceActor, "ResourceActorThread");
		resourceActorThread.start();
	}
}
