package de.qabel.qabelbox.storage;

import android.test.AndroidTestCase;
import android.util.Log;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.qabel.core.crypto.CryptoUtils;
import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.qabelbox.R;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by cdemon on 15.12.2015.
 */
public class SearchTest extends AndroidTestCase {

    private final static String TAG = "#######################";
    private final static String bucket = "qabel";
    private final static String prefix = UUID.randomUUID().toString();

    private static AmazonS3Client s3Client;
    private static List<BoxObject> searchResults;
    private static boolean setup = true;

    @Before
    public void setUp() throws Exception {
        if(!setup) {
            //init this only once, and since @BeforeClass has to be static use this hack
            return;
        }

        setup = false;

        CryptoUtils utils = new CryptoUtils();
        byte[] deviceID = utils.getRandomBytes(16);
        QblECKeyPair keyPair = new QblECKeyPair();

        AWSCredentials awsCredentials = new AWSCredentials() {
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

        TransferUtility transfer = new TransferUtility(s3Client, getContext());
        BoxVolume volume = new BoxVolume(transfer, credentials, keyPair, bucket, prefix, deviceID,
                getContext());

        volume.createIndex(bucket, prefix);

        Log.w(TAG, "VOL :" + volume.toString());

        BoxNavigation nav = volume.navigate();

        setupFakeDirectoryStructure(nav);

        setupBaseSearch(nav);

        Log.w(TAG, "SETUP DONE");
        Log.w(TAG, "--------");
    }

    @AfterClass
    public static void cleanUp() throws IOException {
        ObjectListing listing = s3Client.listObjects(bucket, prefix);
        List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<>();
        for (S3ObjectSummary summary : listing.getObjectSummaries()) {
            Log.w(TAG, "DELETE: " + summary.getKey());
            keys.add(new DeleteObjectsRequest.KeyVersion(summary.getKey()));
        }
        if (keys.isEmpty()) {
            return;
        }
        DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucket);
        deleteObjectsRequest.setKeys(keys);
        s3Client.deleteObjects(deleteObjectsRequest);

        Log.w(TAG, "-------");
    }

    private void setupFakeDirectoryStructure(BoxNavigation nav) throws Exception {

        String testFile = BoxTest.createTestFile();

        assertThat(nav.listFiles().size(), is(0));

        nav.upload("level0-one.bin", new FileInputStream(testFile), null);
        nav.commit();

        BoxFolder folder = nav.createFolder("dir1-level1-one");
        nav.commit();
        nav.navigate(folder);

        nav.upload("level1-ONE.bin", new FileInputStream(testFile), null);
        nav.commit();
        nav.upload("level1-two-Small.bin", new FileInputStream(BoxTest.smallTestFile().getAbsolutePath()), null);
        nav.commit();

        folder = nav.createFolder("dir1-level2-one");
        nav.commit();
        nav.navigate(folder);

        nav.upload("one-level2-one.bin", new FileInputStream(testFile), null);
        nav.commit();

        nav.navigateToParent();

        folder = nav.createFolder("dir1-level2-two");
        nav.commit();
        nav.navigate(folder);

        nav.upload("two-level2-one.bin", new FileInputStream(testFile), null);
        nav.commit();

        //level0-one.bin
        //+dir1-level1-one
        //  level1-ONE.bin
        //  level1-two-Small.bin
        //  +dir1-level2-one
        //      one-level2-one.bin
        //  +dir1-level2-two
        //      two-level2-one.bin

        while(nav.hasParent()) {
            nav.navigateToParent();
        }

        Log.w(TAG, "NAV : "+nav);

        debug(nav);

    }

    private void debug(BoxNavigation nav) throws Exception {

        for(BoxFile file : nav.listFiles()) {
            Log.w(TAG, "FILE: "+file.name);
        }

        for(BoxFolder folder : nav.listFolders()) {
            Log.w(TAG, "DIR : "+folder.name);

            nav.navigate(folder);
            debug(nav);
            nav.navigateToParent();
        }
    }

    private void setupBaseSearch(BoxNavigation nav) throws Exception {
        searchResults = new StorageSearch(nav).getResults();
    }

    @Test
    public void testCollectAll() throws Exception {

        Log.w(TAG, "collectAll");

        for(BoxObject o : searchResults) {
            Log.w(TAG, o instanceof BoxFile ? "FILE: " + o.name : "DIR : " + o.name);
        }

        assertEquals(8, searchResults.size());

        Log.w(TAG, "/collectAll");
    }

    @Test
    public void testForValidName() throws Exception {
        assertFalse(StorageSearch.isValidSearchTerm(null));
        assertFalse(StorageSearch.isValidSearchTerm(""));
        assertFalse(StorageSearch.isValidSearchTerm(" "));

        assertTrue(StorageSearch.isValidSearchTerm("1"));
    }

    @Test
    public void testNameSearch() throws Exception {
        List<BoxObject> lst = new StorageSearch(searchResults).filterByName("small").getResults();

        assertEquals(1, lst.size());
        assertEquals("level1-two-Small.bin", lst.get(0).name);

        assertEquals(0, new StorageSearch(searchResults).filterByNameCaseSensitive("small").getResults().size());
        assertEquals(1, new StorageSearch(searchResults).filterByNameCaseSensitive("Small").getResults().size());

        //if not valid don't apply the filter
        assertEquals(8, new StorageSearch(searchResults).filterByName(null).getResults().size());
        assertEquals(8, new StorageSearch(searchResults).filterByName("").getResults().size());
        assertEquals(8, new StorageSearch(searchResults).filterByName(" ").getResults().size());

    }








}
