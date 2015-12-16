package de.qabel.qabelbox.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import de.qabel.ackack.event.EventEmitter;
import de.qabel.core.config.Account;
import de.qabel.core.config.Contact;
import de.qabel.core.config.Contacts;
import de.qabel.core.config.DropServer;
import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.config.LocalSettings;
import de.qabel.core.config.LocaleModuleSettings;
import de.qabel.core.config.Persistable;
import de.qabel.core.config.Persistence;
import de.qabel.core.config.ResourceActor;
import de.qabel.core.config.Settings;
import de.qabel.core.config.SyncedModuleSettings;
import de.qabel.core.config.SyncedSettings;
import de.qabel.core.crypto.BinaryDropMessageV0;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.drop.DropURL;
import de.qabel.core.exceptions.QblDropPayloadSizeException;
import de.qabel.core.exceptions.QblInvalidEncryptionKeyException;
import de.qabel.core.http.DropHTTP;
import de.qabel.core.http.HTTPResult;
import de.qabel.qabelbox.QabelBoxApplication;
import de.qabel.qabelbox.config.AndroidPersistence;
import de.qabel.qabelbox.config.QblSQLiteParams;

public class LocalQabelService extends Service {

	private static final String TAG = "LocalQabelService";
	private static final String PREF_LAST_ACTIVE_IDENTITY = "PREF_LAST_ACTIVE_IDENTITY";
	private static final char[] PASSWORD = "constantpassword".toCharArray();
	private final IBinder mBinder = new LocalBinder();

	static final String DB_NAME = "qabel-service";
	private static final int DB_VERSION = 1;
	private AndroidPersistence persistence;
	private Identity activeIdentity;
	private SharedPreferences sharedPreferences;

	private void setLastActiveIdentityID(String identityID) {
		sharedPreferences.edit()
				.putString(PREF_LAST_ACTIVE_IDENTITY, identityID)
				.apply();
	}

	private String getLastActiveIdentityID() {
		return sharedPreferences.getString(PREF_LAST_ACTIVE_IDENTITY, "");
	}


	public ResourceActor getResourceActor() {
		return null;
	}

	public void addIdentity(Identity identity) {
		persistence.updateOrPersistEntity(identity);
	}

	public SharedPreferences getSharedPreferences() {
		return sharedPreferences;
	}

	public Identities getIdentities() {
		List<Persistable> entities = persistence.getEntities(Identity.class);
		Identities identities = new Identities();
		for (Persistable p : entities) {
			identities.put((Identity) p);
		}
		return identities;
	}

	public Identity getActiveIdentity() {
		String identityID = getLastActiveIdentityID();
		return getIdentities().getByKeyIdentifier(identityID);
	}

	public void setActiveIdentity(Identity identity) {
		setLastActiveIdentityID(identity.getKeyIdentifier());
	}

	public void deleteIdentity(Identity identity) {
		persistence.removeEntity(identity.getPersistenceID(), Identity.class);
	}

	public void modifyIdentity(Identity identity) {
		persistence.updateEntity(identity);
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
		sharedPreferences = getSharedPreferences(this.getClass().getCanonicalName(), MODE_PRIVATE);
		AndroidPersistence androidPersistence;
		QblSQLiteParams params = new QblSQLiteParams(this, DB_NAME, null, DB_VERSION);
		try {
			androidPersistence = new AndroidPersistence(params, PASSWORD);
		} catch (QblInvalidEncryptionKeyException e) {
			Log.e(TAG, "Invalid database password!");
			return;
		}
		this.persistence = androidPersistence;

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

}

