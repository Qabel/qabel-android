package de.qabel.qabelbox;

import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;

import de.qabel.qabelbox.providers.DocumentIdParser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;


public class DocumentIdParserTest {

    public static final String SEP = "::::";
    private DocumentIdParser p;
    private String pub = "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a";
    private String prefix = "D7A75A70-8D28-11E5-A8EB-280369A460B9";
    private String rootId = pub + SEP + prefix;
    private String filePath = "foo/bar/baz/";
    private String fileName = "lorem.txt";
    private String dottedPath = "::::/foo/bar/baz/";
    private String dottedId = rootId + SEP + "::::/foo/bar/baz/";

    @Before
    public void setUp() {
        p = new DocumentIdParser();
    }

    @Test
    public void testExtractIdentity() throws FileNotFoundException {
        assertThat(p.getIdentity(rootId), is(pub));
    }

    @Test(expected = FileNotFoundException.class)
    public void testNoIdentity() throws FileNotFoundException {
        p.getIdentity("::::foobar");
    }

    @Test
    public void testExtractPrefix() throws FileNotFoundException {
        assertThat(p.getPrefix(rootId), is(prefix));
    }

    @Test(expected = FileNotFoundException.class)
    public void testNoPrefix() throws FileNotFoundException {
        p.getPrefix("foo::::");
    }


    @Test
    public void testExtractFilePath() throws FileNotFoundException {
        assertThat(p.getFilePath(rootId + "::::" + filePath + fileName), is(filePath + fileName));

    }

    @Test
    public void testExtractBaseName() throws FileNotFoundException {
        assertThat(p.getBaseName(rootId + "::::" + filePath + fileName), is(fileName));
    }

    @Test(expected = FileNotFoundException.class)
    public void testNoFilePath() throws FileNotFoundException {
        p.getFilePath(rootId);
    }

    @Test
    public void testBuildId() {
        assertThat(p.buildId(pub, prefix, filePath), is(rootId + SEP + filePath));
        assertThat(p.buildId(pub, prefix, null), is(rootId));
        assertThat(p.buildId(pub, null, null), is(pub));
        assertThat(p.buildId(null, null, null), nullValue());
    }

    @Test
    public void testGetPath() throws FileNotFoundException {
        assertThat(p.getPath(rootId + SEP + filePath + fileName), is(filePath));
    }

    @Test
    public void testPathWithToken() throws Exception {
        assertThat(p.getPath(dottedId), is(dottedPath));
    }

    @Test
    public void testBasenameWithToken() throws Exception {
        assertThat(p.getBaseName(dottedId + fileName), is(fileName));
    }


    @Test
    public void testFilePathWithToken() throws Exception {
        assertThat(p.getFilePath(dottedId + fileName), is(dottedPath + fileName));
        assertThat(p.getFilePath(dottedId + SEP), is(dottedPath + SEP));
    }

    @Test
    public void testPrefixWithToken() throws Exception {
        assertThat(p.getPrefix(dottedId), is(prefix));
    }
}

