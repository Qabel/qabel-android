package de.qabel.qabelbox.providers;

import android.Manifest;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;

import org.spongycastle.util.encoders.Hex;

import java.util.UUID;

import de.qabel.core.config.Identities;
import de.qabel.core.config.Identity;
import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.exceptions.QblInvalidEncryptionKeyException;
import de.qabel.qabelbox.config.AndroidPersistence;
import de.qabel.qabelbox.config.QblSQLiteParams;
import de.qabel.qabelbox.services.LocalQabelService;
import de.qabel.qabelbox.storage.BoxUploadingFile;

public class BoxProviderTester extends BoxProvider {

	public byte[] deviceID;
	public String lastID;
	public QblECKeyPair keyPair;
	public String rootDocId;
	public final String prefix = UUID.randomUUID().toString();
	public static final String PUB_KEY = "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a";
	public static final String PRIVATE_KEY = "77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a";
	public Identity identity;


	@Override
	void bindToService(final Context context) {
		setParametersForTests();
		initServiceForTests(context);
		attachInfoForTests(context);
	}

	private void setParametersForTests() {
		CryptoUtils utils = new CryptoUtils();
		deviceID = utils.getRandomBytes(16);
		keyPair = new QblECKeyPair(Hex.decode(PRIVATE_KEY));
		rootDocId = PUB_KEY + BoxProvider.DOCID_SEPARATOR + BoxProvider.BUCKET
				+ BoxProvider.DOCID_SEPARATOR + prefix + BoxProvider.DOCID_SEPARATOR
				+ BoxProvider.PATH_SEP;
		identity = new Identity("testuser", null, keyPair);
	}

	private void initServiceForTests(Context context) {
		mService = new MockedLocalQabelService(context);
		mService.onCreate();
	}

	private void attachInfoForTests(Context context) {
		ProviderInfo info = new ProviderInfo();
		info.authority = AUTHORITY;
		info.exported = true;
		info.grantUriPermissions = true;
		info.readPermission = Manifest.permission.MANAGE_DOCUMENTS;
		info.writePermission = Manifest.permission.MANAGE_DOCUMENTS;
		attachInfo(context, info);
	}


	private class MockedLocalQabelService extends LocalQabelService {

		private final Context context;

		public MockedLocalQabelService(Context context) {
			this.context = context;
		}

		@Override
		public byte[] getDeviceID() {
			return deviceID;
		}

		@Override
		protected void setLastActiveIdentityID(String identityID) {
			lastID = identityID;
		}

		@Override
		protected String getLastActiveIdentityID() {
			return lastID;
		}

		@Override
		protected void initSharedPreferences() {
		}

		@Override
		protected void initAndroidPersistence() {
			AndroidPersistence androidPersistence;
			QblSQLiteParams params = new QblSQLiteParams(context, DB_NAME, null, DB_VERSION);
			try {
				androidPersistence = new AndroidPersistence(params);
			} catch (QblInvalidEncryptionKeyException e) {
				return;
			}
			this.persistence = androidPersistence;
		}

		@Override
		public Identities getIdentities() {
			Identities identities = new Identities();
			identities.put(identity);
			return identities;
		}

		@Override
		protected void showNotification(String contentTitle, String contentText, int progress) {
		}

		@Override
		protected void updateNotification() {
		}

		@Override
		protected void broadcastUploadStatus(String documentId, int uploadStatus, @Nullable Bundle extras) {
		}
	}
}

