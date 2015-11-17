package de.qabel.qabelbox;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.spongycastle.util.encoders.Hex;

import java.io.FileNotFoundException;

import de.qabel.core.crypto.QblECKeyPair;
import de.qabel.qabelbox.providers.DocumentIdParser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.AndroidJUnitRunner;


@RunWith(AndroidJUnit4.class)
public class DocumentIdParserTest extends TestCase {

    private DocumentIdParser p;
    private QblECKeyPair key;
    private String pub;
    private String prefix;
    private String bucket;
    private String rootId ;
    private String filePath = "foo/bar/baz/";
    private String fileName = "lorem.txt";

    @Before
    public void setUp() {
        p = new DocumentIdParser();
        key = new QblECKeyPair(Hex.decode(
           "77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a"));
        pub = "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a";
        prefix = "D7A75A70-8D28-11E5-A8EB-280369A460B9";
        bucket = "qabel";
        rootId = pub + "::::" + bucket + "::::" + prefix;
    }

    @Test
    public void testExtractIdentity() throws FileNotFoundException {
        assertThat(p.getIdentity(rootId), is(pub));
    }

    @Test(expected = FileNotFoundException.class)
    public void testNoIdentity() throws FileNotFoundException {
        p.getIdentity("foobar");
    }

    @Test
    public void testExtractPrefix() throws FileNotFoundException {
        assertThat(p.getPrefix(rootId), is(prefix));
    }

    @Test(expected = FileNotFoundException.class)
    public void testNoPrefix() throws FileNotFoundException {
        p.getPrefix("foo::::bar");
    }

    @Test
    public void testExtractBucket() throws FileNotFoundException {
        assertThat(p.getBucket(rootId), is(bucket));
        assertThat(p.getBucket(rootId+filePath), is(bucket));
        assertThat(p.getBucket(rootId+filePath+fileName), is(bucket));
    }

    @Test(expected = FileNotFoundException.class)
    public void testNoBucket() throws FileNotFoundException {
        p.getBucket("foo::::bar");

    }
    @Test
    public void testExtractFilePath() throws FileNotFoundException {
        assertThat(p.getFilePath(rootId + "::::" + filePath + fileName), is(filePath + fileName));

    }

    @Test(expected = FileNotFoundException.class)
    public void testNoFilePath() throws FileNotFoundException {
        p.getFilePath(rootId);
    }

    @Test
    public void testBuildId() {
        assertThat(p.buildId(pub, bucket, prefix, filePath), is(rootId + "::::" + filePath));
        assertThat(p.buildId(pub, bucket, prefix, null), is(rootId));
        assertThat(p.buildId(pub, bucket, null, null), is(pub + "::::" + bucket));
        assertThat(p.buildId(pub, null, null, null), is(pub));
        assertNull(p.buildId(null, null, null, null));
    }

}
