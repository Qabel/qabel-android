package de.qabel.core.storage;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.util.IOUtils;
import de.qabel.core.crypto.DecryptedPlaintext;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.exceptions.QblStorageException;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.InvalidKeyException;

public class IndexNavigation extends AbstractNavigation {

	private static final Logger logger = LoggerFactory.getLogger(IndexNavigation.class.getName());

	public IndexNavigation(DirectoryMetadata dm, QblECKeyPair keyPair, byte[] deviceId, TransferUtility transferUtility) {
		super(dm, keyPair, deviceId, transferUtility);
	}

	@Override
	protected DirectoryMetadata reloadMetadata() throws QblStorageException {
		// TODO: duplicate with BoxVoume.navigate()
		String rootRef = dm.getFileName();
		InputStream indexDl = blockingDownload(rootRef);
		File tmp;
		try {
			byte[] encrypted = IOUtils.toByteArray(indexDl);
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
			blockingUpload(dm.getFileName(), new ByteArrayInputStream(encrypted),
					(long) encrypted.length);
			logger.info("Uploading metadata file with name " + dm.getFileName());
		} catch (IOException | InvalidKeyException e) {
			throw new QblStorageException(e);
		}
	}
}
