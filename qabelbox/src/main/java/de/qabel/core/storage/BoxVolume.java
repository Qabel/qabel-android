package de.qabel.core.storage;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;

import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.crypto.DecryptedPlaintext;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.exceptions.QblStorageException;
import de.qabel.core.exceptions.QblStorageNotFound;

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
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;

public class BoxVolume {

	private static final Logger logger = LoggerFactory.getLogger(BoxVolume.class.getName());

	private TransferUtility transferUtility;
	private QblECKeyPair keyPair;
	private String bucket;
	private String prefix;
	private byte[] deviceId;
	private CryptoUtils cryptoUtils;
	private File tempDir;
	private final TransferManager transferManager;

	public BoxVolume(TransferUtility transferUtility, AWSCredentials credentials,
					 QblECKeyPair keyPair, String bucket, String prefix,
					 byte[] deviceId, File tempDir) {
		this.transferUtility = transferUtility;
		this.keyPair = keyPair;
		this.bucket = bucket;
		this.prefix = prefix;
		this.deviceId = deviceId;
		cryptoUtils = new CryptoUtils();
		this.tempDir = tempDir;
		AmazonS3Client awsClient = new AmazonS3Client(credentials);
		transferManager = new TransferManager(transferUtility, awsClient, bucket, prefix, tempDir);
	}

	private InputStream blockingDownload(String name) throws QblStorageNotFound {
		File tmp = transferManager.createTempFile();
		int id = transferManager.download(name, tmp);
		if (transferManager.waitFor(id)) {
			try {
				return new FileInputStream(tmp);
			} catch (FileNotFoundException e) {
				throw new QblStorageNotFound("Download failed");
			}
		} else {
			throw new QblStorageNotFound("Download failed");
		}
	}

	private void blockingUpload(String name,
								InputStream inputStream) throws QblStorageException {
		File tmp = transferManager.createTempFile();
		try {
			IOUtils.copy(inputStream, new FileOutputStream(tmp));
		} catch (IOException e) {
			throw new QblStorageException(e);
		}
		int id = transferManager.upload(name, tmp);
		if (!transferManager.waitFor(id)) {
			throw new QblStorageException("Upload failed");
		}
	}


	public BoxNavigation navigate() throws QblStorageException {
		String rootRef = getRootRef();
		logger.info("Navigating to " + rootRef);
		InputStream indexDl = blockingDownload(rootRef);
		File tmp;
		try {
			byte[] encrypted = IOUtils.toByteArray(indexDl);
			if (encrypted.length == 0) {
				throw new QblStorageException("FUCK");
			}
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
		return new IndexNavigation(dm, keyPair, deviceId, transferManager);
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
			blockingUpload(rootRef, new ByteArrayInputStream(encrypted));
		} catch (IOException e) {
			throw new QblStorageException(e);
		} catch (InvalidKeyException e) {
			throw new QblStorageException(e);
		}


	}
}
