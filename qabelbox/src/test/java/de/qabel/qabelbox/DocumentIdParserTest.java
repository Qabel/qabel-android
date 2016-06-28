package de.qabel.qabelbox;

import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;

import de.qabel.qabelbox.providers.BoxProvider;
import de.qabel.qabelbox.providers.DocumentId;
import de.qabel.qabelbox.providers.DocumentIdParser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;


public class DocumentIdParserTest {

    public static final String SEP = "::::";
    private DocumentIdParser documentIdParser;
    private String pub = "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a";
    private String prefix = "D7A75A70-8D28-11E5-A8EB-280369A460B9";
    private String rootId = pub + SEP + prefix;
    private String filePath = "foo/bar/baz";
    private String fileName = "lorem.txt";
    private String dottedPath = "::::/foo/bar/baz";
    private String dottedId = rootId + SEP + filePath + BoxProvider.PATH_SEP + fileName;

    @Before
    public void setUp() {
        documentIdParser = new DocumentIdParser();
    }

    @Test
    public void testExtractIdentity() throws FileNotFoundException {
        assertThat(documentIdParser.getIdentity(rootId), is(pub));
    }

    @Test(expected = FileNotFoundException.class)
    public void testNoIdentity() throws FileNotFoundException {
        documentIdParser.getIdentity("::::foobar");
    }

    @Test
    public void testExtractPrefix() throws FileNotFoundException {
        assertThat(documentIdParser.getPrefix(rootId), is(prefix));
    }

    @Test(expected = FileNotFoundException.class)
    public void testNoPrefix() throws FileNotFoundException {
        documentIdParser.getPrefix("foo::::");
    }


    @Test
    public void testExtractFilePath() throws FileNotFoundException {
        assertThat(documentIdParser.getFilePath(rootId + "::::" + filePath + fileName), is(filePath + fileName));

    }

    @Test
    public void testExtractBaseName() throws FileNotFoundException {
        assertThat(documentIdParser.getBaseName(dottedId), is(fileName));
    }

    @Test(expected = FileNotFoundException.class)
    public void testNoFilePath() throws FileNotFoundException {
        documentIdParser.getFilePath(rootId);
    }

    @Test
    public void testBuildId() {
        assertThat(documentIdParser.buildId(pub, prefix, filePath), is(rootId + SEP + filePath));
        assertThat(documentIdParser.buildId(pub, prefix, null), is(rootId));
        assertThat(documentIdParser.buildId(pub, null, null), is(pub));
    }

    @Test
    public void testGetPath() throws FileNotFoundException {
        assertThat(documentIdParser.getPath(dottedId), is(filePath + BoxProvider.PATH_SEP));
    }

    @Test
    public void testPathWithToken() throws Exception {
        assertThat(documentIdParser.getPath(dottedId), is(filePath + BoxProvider.PATH_SEP));
    }

    @Test
    public void testBasenameWithToken() throws Exception {
        assertThat(documentIdParser.getBaseName(dottedId), is(fileName));
    }


    @Test
    public void testFilePathWithToken() throws Exception {
        assertThat(documentIdParser.getFilePath(dottedId), is(filePath + BoxProvider.PATH_SEP + fileName));
        assertThat(documentIdParser.getFilePath(dottedId + SEP), is(filePath + BoxProvider.PATH_SEP + fileName + SEP));
    }

    @Test
    public void testPrefixWithToken() throws Exception {
        assertThat(documentIdParser.getPrefix(dottedId), is(prefix));
    }

    @Test
    public void testParseDocumentID() throws Exception {
        DocumentId documentId = documentIdParser.parse(dottedId);

        assertThat(documentId.getIdentityKey(), is(pub));
        assertThat(documentId.getPrefix(), is(prefix));

        assertThat(documentId.getFileName(), is(fileName));
        assertThat(documentId.getPathString(), is(filePath));
    }

    @Test
    public void testDocumentId() throws Exception {
        assertThat(dottedId, is(documentIdParser.parse(dottedId).toString()));
    }
}

