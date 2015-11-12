package de.qabel.core.storage;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;

import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.exceptions.QblStorageException;
import de.qabel.core.exceptions.QblStorageNotFound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.InvalidKeyException;

public class FolderNavigation extends AbstractNavigation {

	private static final Logger logger = LoggerFactory.getLogger(FolderNavigation.class.getName());

	private final byte[] key;

	public FolderNavigation(DirectoryMetadata dm, QblECKeyPair keyPair, byte[] key, byte[] deviceId, TransferUtility transferUtility) {
		super(dm, keyPair, deviceId, transferUtility);
		this.key = key;
	}

	@Override
	protected void uploadDirectoryMetadata() throws QblStorageException {
		logger.info("Uploading directory metadata");
		SecretKey secretKey = new SecretKeySpec(key, "AES");
		try {
			uploadEncrypted(new FileInputStream(dm.getPath()), secretKey, dm.getFileName());
		} catch (FileNotFoundException e) {
			throw new QblStorageException(e);
		}
	}

	@Override
	protected DirectoryMetadata reloadMetadata() throws QblStorageException {
		logger.info("Reloading directory metadata");
		// duplicate of navigate()
		try {
			InputStream indexDl = blockingDownload(dm.getFileName());
			File tmp = File.createTempFile("dir", "db", dm.getTempDir());
			SecretKey key = makeKey(this.key);
			if (cryptoUtils.decryptFileAuthenticatedSymmetricAndValidateTag(indexDl, tmp, key)) {
				return DirectoryMetadata.openDatabase(tmp, deviceId, dm.getFileName(), dm.getTempDir());
			} else {
				throw new QblStorageNotFound("Invalid key");
			}
		} catch (IOException | InvalidKeyException e) {
			throw new QblStorageException(e);
		}
	}
}
