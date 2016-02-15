package de.qabel.qabelbox.storage;





import android.test.AndroidTestCase;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.core.crypto.QblECPublicKey;
import de.qabel.qabelbox.R;
import de.qabel.qabelbox.exceptions.QblStorageException;
import de.qabel.qabelbox.exceptions.QblStorageNameConflict;
import de.qabel.qabelbox.exceptions.QblStorageNotFound;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class BoxTest extends AndroidTestCase {
    private static final Logger logger = LoggerFactory.getLogger(BoxTest.class.getName());
	private static final QblECPublicKey OWNER = new QblECKeyPair().getPub();

	BoxVolume volume;
    BoxVolume volume2;
	BoxVolume volumeOtherUser;
    byte[] deviceID;
    byte[] deviceID2;
	byte[] deviceIDOtherUser;
	QblECKeyPair keyPair;
	QblECKeyPair keyPairOtherUser;
    final String bucket = "qabel";
    final String prefix = UUID.randomUUID().toString();
	final String prefixOtherUser = UUID.randomUUID().toString();
    private String testFileName;
    private AmazonS3Client s3Client;
    private AWSCredentials awsCredentials;

    public void setUp() throws IOException, QblStorageException {
        CryptoUtils utils = new CryptoUtils();
        deviceID = utils.getRandomBytes(16);
        deviceID2 = utils.getRandomBytes(16);
		deviceIDOtherUser = utils.getRandomBytes(16);


		testFileName = createTestFile();

        keyPair = new QblECKeyPair();
		keyPairOtherUser = new QblECKeyPair();

        awsCredentials = new AWSCredentials() {
            @Override
            public String getAWSAccessKeyId() {
                return getContext().getResources().getString(R.string.aws_user);
            }

            @Override
            public String getAWSSecretKey() {
                return getContext().getString(R.string.aws_password);
            }
        };
        AWSCredentials credentials = awsCredentials;
        s3Client = new AmazonS3Client(credentials);
        assertNotNull(awsCredentials.getAWSAccessKeyId());
        assertNotNull(awsCredentials.getAWSSecretKey());

        QblECKeyPair keyPair = new QblECKeyPair();
        TransferUtility transfer = new TransferUtility(s3Client, getContext());
        volume = new BoxVolume(transfer, credentials, keyPair, bucket, prefix, deviceID,
                getContext());
        volume2 = new BoxVolume(transfer, credentials, keyPair, bucket, prefix, deviceID2,
                getContext());
		volumeOtherUser = new BoxVolume(transfer, credentials, keyPairOtherUser, bucket, prefixOtherUser,
				deviceIDOtherUser, getContext());

        volume.createIndex(bucket, prefix);
		volumeOtherUser.createIndex(bucket, prefixOtherUser);

    }

    public static String createTestFile() throws IOException {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File file = File.createTempFile("testfile", "test", tmpDir);
        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] testData = new byte[1024];
        Arrays.fill(testData, (byte) 'f');
        for (int i = 0; i < 100; i++) {
            outputStream.write(testData);
        }
        outputStream.close();
        return file.getAbsolutePath();
    }

    public static File smallTestFile() throws IOException {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File file = File.createTempFile("testfile", "test", tmpDir);
        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] testData = new byte[] {1,2,3,4,5};
		outputStream.write(testData);
        outputStream.close();
        return file;
    }

    public void tearDown() throws IOException {
		deleteObjects(bucket, prefix);
		deleteObjects(bucket, prefixOtherUser);
    }

	private void deleteObjects(String bucket, String prefix) {
		ObjectListing listing = s3Client.listObjects(bucket, prefix);
		List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<>();
		for (S3ObjectSummary summary : listing.getObjectSummaries()) {
			logger.info("deleting key" + summary.getKey());
			keys.add(new DeleteObjectsRequest.KeyVersion(summary.getKey()));
		}
		if (keys.isEmpty()) {
			return;
		}
		DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucket);
		deleteObjectsRequest.setKeys(keys);

	}

    @Test
    public void testCreateIndex() throws QblStorageException {
        BoxNavigation nav = volume.navigate();
        assertThat(nav.listFiles().size(), is(0));
    }

    @Test
    public void testUploadFile() throws QblStorageException, IOException {
        uploadFile(volume.navigate());
    }

    @Test
	public void testShareFile() throws QblStorageException, IOException {
		BoxNavigation nav = volume.navigate();
		File file = new File(testFileName);
		BoxFile boxFile = nav.upload("foobar", new FileInputStream(file), null);
		nav.commit();

		nav.createFileMetadata(OWNER, boxFile);
		nav.commit();

		// Share meta and metakey to other user

		BoxNavigation navOtherUser = volumeOtherUser.navigate();
		navOtherUser.attachExternal(false, boxFile.name, OWNER, boxFile.meta, boxFile.metakey);
		navOtherUser.commit();

		List<BoxObject> boxExternalFiles = navOtherUser.listExternals();
		assertThat(boxExternalFiles.size(), is(1));
		assertTrue(boxExternalFiles.get(0) instanceof BoxExternalFile);
		BoxExternalFile boxFileReceived = (BoxExternalFile) boxExternalFiles.get(0);
		assertThat(boxFile.name, is(equalTo(boxFileReceived.name)));
		assertThat(boxFile.key, is(equalTo(boxFileReceived.key)));
		assertThat(OWNER, is(equalTo(boxFileReceived.owner)));
	}

	@Test
	public void testShareAndUpdateFile() throws QblStorageException, IOException {
		BoxNavigation nav = volume.navigate();
		File file = new File(testFileName);
		BoxFile boxFile = nav.upload("foobar", new FileInputStream(file), null);
		nav.commit();

		nav.createFileMetadata(OWNER, boxFile);
		nav.commit();

		// Share meta and metakey to other user

		BoxNavigation navOtherUser = volumeOtherUser.navigate();
		navOtherUser.attachExternal(false, boxFile.name, OWNER, boxFile.meta, boxFile.metakey);
		navOtherUser.commit();

		List<BoxObject> boxExternalFiles = navOtherUser.listExternals();
		assertThat(boxExternalFiles.size(), is(1));
		assertTrue(boxExternalFiles.get(0) instanceof BoxExternalFile);
		BoxExternalFile boxFileReceived = (BoxExternalFile) boxExternalFiles.get(0);
		assertThat(boxFile.name, is(equalTo(boxFileReceived.name)));
		assertThat(boxFile.key, is(equalTo(boxFileReceived.key)));
		assertThat(OWNER, is(equalTo(boxFileReceived.owner)));

		boxFile = nav.upload("foobar", new FileInputStream(file), null);
		nav.commit();

		// Check that updated file can still be read

		boxExternalFiles = navOtherUser.listExternals();
		assertThat(boxExternalFiles.size(), is(1));
		assertTrue(boxExternalFiles.get(0) instanceof BoxExternalFile);
		boxFileReceived = (BoxExternalFile) boxExternalFiles.get(0);
		assertThat(boxFile.name, is(equalTo(boxFileReceived.name)));
		assertThat(boxFile.key, is(equalTo(boxFileReceived.key)));
		assertThat(OWNER, is(equalTo(boxFileReceived.owner)));
	}

	@Test
	public void testShareAndUpdateAndUnshareFile() throws QblStorageException, IOException {
		BoxNavigation nav = volume.navigate();
		File file = new File(testFileName);
		BoxFile boxFile = nav.upload("foobar", new FileInputStream(file), null);
		nav.commit();

		nav.createFileMetadata(OWNER, boxFile);
		nav.commit();

		// Share meta and metakey to other user

		BoxNavigation navOtherUser = volumeOtherUser.navigate();
		navOtherUser.attachExternal(false, boxFile.name, OWNER, boxFile.meta, boxFile.metakey);
		navOtherUser.commit();

		List<BoxObject> boxExternalFiles = navOtherUser.listExternals();
		assertThat(boxExternalFiles.size(), is(1));
		assertTrue(boxExternalFiles.get(0) instanceof BoxExternalFile);
		BoxExternalFile boxFileReceived = (BoxExternalFile) boxExternalFiles.get(0);
		assertThat(boxFile.name, is(equalTo(boxFileReceived.name)));
		assertThat(boxFile.key, is(equalTo(boxFileReceived.key)));
		assertThat(OWNER, is(equalTo(boxFileReceived.owner)));

		boxFile = nav.upload("foobar", new FileInputStream(file), null);
		nav.commit();

		// Check that updated file can still be read

		boxExternalFiles = navOtherUser.listExternals();
		assertThat(boxExternalFiles.size(), is(1));
		assertTrue(boxExternalFiles.get(0) instanceof BoxExternalFile);
		boxFileReceived = (BoxExternalFile) boxExternalFiles.get(0);
		assertThat(boxFile.name, is(equalTo(boxFileReceived.name)));
		assertThat(boxFile.key, is(equalTo(boxFileReceived.key)));
		assertThat(OWNER, is(equalTo(boxFileReceived.owner)));

		// Remove FileMetadata and update file
		nav.removeFileMetadata(boxFile);

		boxFile = nav.upload("foobar", new FileInputStream(file), null);
		nav.commit();

		// Check that updated file cannot be read anymore

		boxExternalFiles = navOtherUser.listExternals();
		assertThat(boxExternalFiles.size(), is(1));
		assertTrue(boxExternalFiles.get(0) instanceof BoxExternalFile);
		boxFileReceived = (BoxExternalFile) boxExternalFiles.get(0);
		assertThat(boxFileReceived.isAccessible(), is(false));
	}

	@Test
	public void testFileIsShared() throws QblStorageException, IOException {
		BoxNavigation nav = volume.navigate();
		File file = new File(testFileName);
		BoxFile boxFile = nav.upload("foobar", new FileInputStream(file), null);
		nav.commit();

		assertThat(boxFile.isShared(), is(false));

		nav.createFileMetadata(OWNER, boxFile);
		nav.commit();

		assertThat(boxFile.isShared(), is(true));

		nav.removeFileMetadata(boxFile);
		nav.commit();

		assertThat(boxFile.isShared(), is(false));
	}

	@Test
	public void testDetachFileMetadataShareFile() throws QblStorageException, IOException {
		BoxNavigation nav = volume.navigate();
		File file = new File(testFileName);
		BoxFile boxFile = nav.upload("foobar", new FileInputStream(file), null);
		nav.commit();

		nav.createFileMetadata(OWNER, boxFile);
		nav.commit();

		// Share meta and metakey to other user

		BoxNavigation navOtherUser = volumeOtherUser.navigate();
		navOtherUser.attachExternal(false, boxFile.name, OWNER, boxFile.meta, boxFile.metakey);
		navOtherUser.commit();

		List<BoxObject> boxExternalFiles = navOtherUser.listExternals();
		assertThat(boxExternalFiles.size(), is(1));

		navOtherUser.detachExternal(boxExternalFiles.get(0).name);
		navOtherUser.commit();

		boxExternalFiles = navOtherUser.listExternals();
		assertThat(boxExternalFiles.size(), is(0));
	}

	@Test
	public void testShareFolder() throws QblStorageException, IOException {
		BoxNavigation nav = volume.navigate();
		File file = new File(testFileName);

		BoxFolder boxFolder = nav.createFolder("SHARE");
		nav.commit();

		nav.navigate(boxFolder);

		BoxExternalReference boxExternalReference = nav.getDmRef(OWNER);

		BoxFile boxFile1 = nav.upload("foobar", new FileInputStream(file), null);
		nav.commit();

		BoxFolder subFolder = nav.createFolder("SUBFOLDER");
		nav.commit();

		nav.navigate(subFolder);

		BoxFile boxFile2 = nav.upload("foobar2", new FileInputStream(file), null);
		nav.commit();

		// Share meta and metakey to other user

		BoxNavigation navOtherUser = volumeOtherUser.navigate();
		navOtherUser.attachExternal(boxExternalReference.isFolder, boxExternalReference.name,
				boxExternalReference.owner, boxExternalReference.url, boxExternalReference.key);
		navOtherUser.commit();

		List<BoxObject> externals = navOtherUser.listExternals();
		assertThat(externals.size(), is(2));

		assertTrue(externals.get(0) instanceof BoxExternalFile);
		BoxExternalFile boxFileReceived = (BoxExternalFile) externals.get(0);
		assertThat(boxFile1.name, is(equalTo(boxFileReceived.name)));
		assertThat(boxFile1.key, is(equalTo(boxFileReceived.key)));
		assertThat(OWNER, is(equalTo(boxFileReceived.owner)));

		assertTrue(externals.get(1) instanceof BoxExternalFolder);
		navOtherUser.navigate(boxExternalReference.url.split("/")[0], (BoxExternalFolder) externals.get(1));

		List<BoxFile> boxFiles = navOtherUser.listFiles();
		assertThat(boxFiles.size(), is(1));

		BoxFile boxFileReceived2 = boxFiles.get(0);
		assertThat(boxFile2.name, is(equalTo(boxFileReceived2.name)));
		assertThat(boxFile2.key, is(equalTo(boxFileReceived2.key)));
	}

	@Test
	public void testShareRootFailure() throws QblStorageException {
		BoxNavigation nav = volume.navigate();
		try {
			nav.getDmRef(OWNER);
		} catch (QblStorageException e) {
			return;
		}
		fail("Expected QblStorageNotFound");
	}

	@Test
	public void testShareDirectoryMetadataWithoutOwnerRootFailure() throws QblStorageException {
		BoxNavigation nav = volume.navigate();
		BoxFolder boxFolder = nav.createFolder("SHARE");
		nav.commit();

		nav.navigate(boxFolder);

		try {
			nav.getDmRef(null);
		} catch (QblStorageException e) {
			return;
		}
		fail("Expected QblStorageNotFound");
	}


	@Test
    public void testDeleteFile() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        BoxFile boxFile = uploadFile(nav);
        nav.delete(boxFile);
        nav.commit();
        try {
            nav.download(boxFile, null);
        } catch (QblStorageNotFound e) {
            return;
        }
        Assert.fail("Expected QblStorageNotFound");
    }

    private BoxFile uploadFile(BoxNavigation nav) throws QblStorageException, IOException {
        File file = new File(testFileName);
        long time = System.currentTimeMillis() / 1000;
        BoxFile boxFile = nav.upload("foobar", new FileInputStream(file), null);
        assertThat(boxFile.mtime, greaterThanOrEqualTo(time));
        assertThat(boxFile.size, not(equalTo(boxFile.mtime)));
        nav.commit();
        checkFile(boxFile, nav);
        return boxFile;
    }

    private void checkFile(BoxFile boxFile, BoxNavigation nav) throws QblStorageException, IOException {
        InputStream dlStream = nav.download(boxFile, null);
        assertNotNull("Download stream is null", dlStream);
        byte[] dl = IOUtils.toByteArray(dlStream);
        File file = new File(testFileName);
        byte[] content = IOUtils.toByteArray(new FileInputStream(file));
        assertThat(dl, is(content));
    }

    @Test
    public void testCreateFolder() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        BoxFolder boxFolder = nav.createFolder("foobdir");
        nav.commit();
        assertThat(nav.getPath(boxFolder), is ("/foobdir/"));

        nav.navigate(boxFolder);
        BoxFile boxFile = uploadFile(nav);
        assertThat(nav.getPath(boxFile), is ("/foobdir/foobar"));

        checkFile(boxFile, nav);

        BoxNavigation nav_new = volume.navigate();
        List<BoxFolder> folders = nav_new.listFolders();
        assertThat(folders.size(), is(1));
        assertThat(boxFolder, equalTo(folders.get(0)));
    }

    @Test
    public void testDeleteFolder() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        BoxFolder boxFolder = nav.createFolder("foobdir");
        nav.commit();

        nav.navigate(boxFolder);
        BoxFile boxFile = uploadFile(nav);
        BoxFolder subfolder = nav.createFolder("subfolder");
        nav.commit();

        nav = volume.navigate();
        nav.delete(boxFolder);
        nav.commit();
        BoxNavigation nav_after = volume.navigate();
        assertThat(nav_after.listFolders().isEmpty(), is(true));
        checkDeleted(boxFolder, subfolder, boxFile, nav_after);
    }

    private void checkDeleted(BoxFolder boxFolder, BoxFolder subfolder, BoxFile boxFile, BoxNavigation nav) throws QblStorageException {
        try {
            nav.download(boxFile, null);
            fail("Could download file in deleted folder");
        } catch (QblStorageNotFound e) { }
        try {
            nav.navigate(boxFolder);
            fail("Could navigate to deleted folder");
        } catch (QblStorageNotFound e) { }
        try {
            nav.navigate(subfolder);
            fail("Could navigate to deleted subfolder");
        } catch (QblStorageNotFound e) { }
    }

    @Test
    public void testOverrideFile() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        uploadFile(nav);
        uploadFile(nav);
        assertThat(nav.listFiles().size(), is(1));
    }

    @Test
    public void testConflictFileUpdate() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        BoxNavigation nav2 = volume2.navigate();
        File file = new File(testFileName);
        nav.upload("foobar", new FileInputStream(file), null);
        nav2.upload("foobar", new FileInputStream(file), null);
        nav2.commit();
        nav.commit();
        assertThat(nav.listFiles().size(), is(2));
    }

    @Test
    public void testFileNameConflict() throws QblStorageException, FileNotFoundException {
        BoxNavigation nav = volume.navigate();
        nav.createFolder("foobar");
        try {
            nav.upload("foobar", new FileInputStream(new File(testFileName)), null);
        } catch (QblStorageNameConflict e) {
            return;
        }
        fail("Expected QblStorageNameConflict");
    }

    @Test
    public void testFolderNameConflict() throws QblStorageException, FileNotFoundException {
        BoxNavigation nav = volume.navigate();
        nav.upload("foobar", new FileInputStream(new File(testFileName)), null);
        try {
            nav.createFolder("foobar");
        } catch (QblStorageNameConflict e) {
            return;
        }
        fail("Expected QblStorageNameConflict");
    }

    @Test
    public void testNavigateToIndirectSubfolder() throws QblStorageException {
        BoxNavigation nav = volume.navigate();
        BoxFolder boxFolder = nav.createFolder("foobdir");
        nav.commit();

        nav.navigate(boxFolder);
        BoxFolder subfolder = nav.createFolder("subfolder");
        nav.commit();

        nav = volume.navigate();
        try {
            nav.navigate(subfolder);
        } catch (QblStorageNotFound e) {
            return;
        }
        fail("Expected QblStorageNotFound");
    }

    @Test
    public void testNavigateToParent() throws QblStorageException {
        BoxNavigation nav = volume.navigate();
        BoxFolder boxFolder = nav.createFolder("foobdir");
        nav.commit();

        nav.navigate(boxFolder);
        BoxFolder subfolder = nav.createFolder("subfolder");
        nav.commit();

        nav.navigate(subfolder);
        BoxFolder subfolder2 = nav.createFolder("subfolder2");
        nav.commit();

        nav = volume.navigate();
        nav.navigate(boxFolder);
        nav.navigate(subfolder);
        nav.navigate(subfolder2);

        assertThat(nav.getPath(), is("/foobdir/subfolder/subfolder2/"));
        nav.navigateToParent();
        assertThat(nav.getPath(), is("/foobdir/subfolder/"));
        nav.navigateToParent();
        assertThat(nav.getPath(), is("/foobdir/"));
        nav.navigateToParent();
        assertThat(nav.getPath(), is("/"));
    }

    @Test
    public void testNavigateToParentOfRoot() throws QblStorageException {
        BoxNavigation nav = volume.navigate();

        try {
            nav.navigateToParent();
        } catch (QblStorageException e) {
            return;
        }
        fail("Expected QblStorageException");
    }

    @Test
    public void testNameConflictOnDifferentClients() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        BoxNavigation nav2 = volume2.navigate();
        File file = new File(testFileName);
        nav.upload("foobar", new FileInputStream(file), null);
        nav2.createFolder("foobar");
        nav2.commit();
        nav.commit();
        assertThat(nav.listFiles().size(), is(1));
        assertThat(nav.listFolders().size(), is(1));
        assertThat(nav.listFiles().get(0).name, startsWith("foobar_conflict"));
    }

    ///**
    // * Currently a folder with a name conflict just disappears and all is lost.
    // */
    //@Test
    //public void testFolderNameConflictOnDifferentClients() throws QblStorageException, IOException {
    //	BoxNavigation nav = volume.navigate();
    //	BoxNavigation nav2 = volume2.navigate();
    //	File file = new File(testFileName);
    //	nav.createFolder("foobar");
    //	nav2.upload("foobar", new FileInputStream(file));
    //	nav2.commit();
    //	nav.commit();
    //	assertThat(nav.listFiles().size(), is(1));
    //	assertThat(nav.listFolders().size(), is(1));
    //	assertThat(nav.listFiles().get(0).name, startsWith("foobar_conflict"));
    //}

    @Test
    public void testUploadLargeFile() throws QblStorageException, IOException {
        BoxNavigation nav = volume.navigate();
        File file = File.createTempFile("large", "testfile");
        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] testData = new byte[1024];
        Arrays.fill(testData, (byte) 'f');
        for (int i=0; i<1000; i++) {
            outputStream.write(testData);
        }
        outputStream.close();
        nav.upload("large file", new FileInputStream(file), null);
    }

    @Test
    public void testCacheFailure() throws QblStorageException, IOException {
        File file = smallTestFile();
        BoxNavigation nav = volume.navigate();
        BoxFile boxFile = nav.upload("foobar", new FileInputStream(file), null);
        nav.commit();

        // warm up cache
        nav.download(boxFile, null);
        corruptCachedFile(boxFile);

        InputStream dlStream = nav.download(boxFile, null);
        assertNotNull("Download stream is null", dlStream);
        byte[] dl = IOUtils.toByteArray(dlStream);
        byte[] content = IOUtils.toByteArray(new FileInputStream(file));
        assertThat("Downloaded file is not correct", dl, is(content));
    }

    private void corruptCachedFile(BoxFile boxFile) throws IOException {
        // corrupt the file
        FileOutputStream outputStream = new FileOutputStream(new FileCache(getContext()).get(boxFile));
        outputStream.write(1);
        outputStream.close();
    }
}
