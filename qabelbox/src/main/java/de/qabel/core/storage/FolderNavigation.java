package de.qabel.core.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;

import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.exceptions.QblStorageException;
import de.qabel.core.exceptions.QblStorageNotFound;

public class FolderNavigation extends AbstractNavigation {

	private static final Logger logger = LoggerFactory.getLogger(FolderNavigation.class.getName());

	private final byte[] key;

	public FolderNavigation(DirectoryMetadata dm, QblECKeyPair keyPair, byte[] key, byte[] deviceId, TransferManager transferUtility) {
		super(dm, keyPair, deviceId, transferUtility);
		this.key = key;
	}

	@Override
	protected void uploadDirectoryMetadata() throws QblStorageException {
		logger.info("Uploading directory metadata");
		try {
			uploadEncrypted(new FileInputStream(dm.getPath()), new KeyParameter(key),
					dm.getFileName(), null);
		} catch (FileNotFoundException e) {
			throw new QblStorageException(e);
		}
	}

	@Override
	protected DirectoryMetadata reloadMetadata() throws QblStorageException {
		logger.info("Reloading directory metadata");
		// duplicate of navigate()
		try {
			File indexDl = blockingDownload(dm.getFileName(), null);
			File tmp = File.createTempFile("dir", "db", dm.getTempDir());
			if (cryptoUtils.decryptFileAuthenticatedSymmetricAndValidateTag(
					new FileInputStream(indexDl), tmp, new KeyParameter(key))) {
				return DirectoryMetadata.openDatabase(tmp, deviceId, dm.getFileName(), dm.getTempDir());
			} else {
				throw new QblStorageNotFound("Invalid key");
			}
		} catch (IOException | InvalidKeyException e) {
			throw new QblStorageException(e);
		}
	}
}
