package de.qabel.qabelbox.storage;

import com.amazonaws.util.IOUtils;
import de.qabel.core.crypto.DecryptedPlaintext;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.qabelbox.exceptions.QblStorageException;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.InvalidKeyException;

public class IndexNavigation extends AbstractNavigation {

	private static final Logger logger = LoggerFactory.getLogger(IndexNavigation.class.getName());

	public IndexNavigation(DirectoryMetadata dm, QblECKeyPair keyPair, byte[] deviceId,
						   TransferManager transferManager) {
		super(dm, keyPair, deviceId, transferManager, "/");
	}

	@Override
	protected DirectoryMetadata reloadMetadata() throws QblStorageException {
		// TODO: duplicate with BoxVoume.navigate()
		String rootRef = dm.getFileName();
		File indexDl = blockingDownload(rootRef, null);
		File tmp;
		try {
			byte[] encrypted = IOUtils.toByteArray(new FileInputStream(indexDl));
			DecryptedPlaintext plaintext = cryptoUtils.readBox(keyPair, encrypted);
			tmp = File.createTempFile("dir", "db", dm.getTempDir());
			logger.info("Using " + tmp.toString() + " for the metadata file");
			OutputStream out = new FileOutputStream(tmp);
			out.write(plaintext.getPlaintext());
			out.close();
		} catch (IOException | InvalidCipherTextException | InvalidKeyException e) {
			throw new QblStorageException(e);
		}
		return DirectoryMetadata.openDatabase(tmp, deviceId, rootRef, dm.getTempDir());
	}

	@Override
	protected void uploadDirectoryMetadata() throws QblStorageException {
		try {
			byte[] plaintext = IOUtils.toByteArray(new FileInputStream(dm.path));
			byte[] encrypted = cryptoUtils.createBox(keyPair, keyPair.getPub(), plaintext, 0);
			File tmp = transferManager.createTempFile();
			FileOutputStream fileOutputStream = new FileOutputStream(tmp);
			fileOutputStream.write(encrypted);
			fileOutputStream.close();
			blockingUpload(dm.getFileName(), tmp, null);
		} catch (IOException | InvalidKeyException e) {
			throw new QblStorageException(e);
		}
	}
}
