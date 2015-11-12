package de.qabel.core.storage;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;

import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.crypto.DecryptedPlaintext;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.exceptions.QblStorageException;
import org.apache.commons.io.IOUtils;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

public class BoxVolume {

	private static final Logger logger = LoggerFactory.getLogger(BoxVolume.class.getName());

	private TransferUtility transferUtility;
	private QblECKeyPair keyPair;
	private String bucket;
	private String prefix;
	private byte[] deviceId;
	private CryptoUtils cryptoUtils;
	private File tempDir;

	public BoxVolume(TransferUtility transferUtility, QblECKeyPair keyPair,
					 String bucket, String prefix,
					 byte[] deviceId, File tempDir) {
		this.transferUtility = transferUtility;
		this.keyPair = keyPair;
		this.bucket = bucket;
		this.prefix = prefix;
		this.deviceId = deviceId;
		cryptoUtils = new CryptoUtils();
		this.tempDir = tempDir;
	}

	private InputStream blockingDownload(String name) {
		return null;
	}


	private void blockingUpload(String rootRef,
								ByteArrayInputStream byteArrayInputStream, long length) {
		return;
	}


	public BoxNavigation navigate() throws QblStorageException {
		String rootRef = getRootRef();
		logger.info("Navigating to " + rootRef);
		InputStream indexDl = blockingDownload(rootRef);
		File tmp;
		try {
			byte[] encrypted = IOUtils.toByteArray(indexDl);
			DecryptedPlaintext plaintext = cryptoUtils.readBox(keyPair, encrypted);
			// Should work fine for the small metafiles
			tmp = File.createTempFile("dir", "db", tempDir);
			OutputStream out = new FileOutputStream(tmp);
			out.write(plaintext.getPlaintext());
			out.close();
		} catch (IOException | InvalidCipherTextException | InvalidKeyException e) {
			throw new QblStorageException(e);
		}
		DirectoryMetadata dm = DirectoryMetadata.openDatabase(tmp, deviceId, rootRef, tempDir);
		return new IndexNavigation(dm, keyPair, deviceId, transferUtility);
	}

	public String getRootRef() throws QblStorageException {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new QblStorageException(e);
		}
		md.update(keyPair.getPrivateKey());
		byte[] digest = md.digest();
		byte[] firstBytes = Arrays.copyOfRange(digest, 0, 16);
		ByteBuffer bb = ByteBuffer.wrap(firstBytes);
		UUID uuid = new UUID(bb.getLong(), bb.getLong());
		return uuid.toString();
	}

	public void createIndex(String bucket, String prefix) throws QblStorageException {
		createIndex("https://" + bucket + ".s3.amazonaws.com/" + prefix);
	}

	public void createIndex(String root) throws QblStorageException {
		String rootRef = getRootRef();
		DirectoryMetadata dm = DirectoryMetadata.newDatabase(root, deviceId, tempDir);
		try {
			byte[] plaintext = IOUtils.toByteArray(new FileInputStream(dm.path));
			byte[] encrypted = cryptoUtils.createBox(keyPair, keyPair.getPub(), plaintext, 0);
			blockingUpload(rootRef, new ByteArrayInputStream(encrypted), (long) encrypted.length);
		} catch (IOException e) {
			throw new QblStorageException(e);
		} catch (InvalidKeyException e) {
			throw new QblStorageException(e);
		}


	}
}
