package de.qabel.qabelbox.providers;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Document IDs are built like this:
 * public-key::::bucket::::prefix::::/filepath
 */
public class DocumentIdParser {

    private static final String DOCID_SEPERATOR = "::::";

    public String getIdentity(String documentId) throws FileNotFoundException {
        String[] split = documentId.split(DOCID_SEPERATOR, 2);
        if (split.length > 1) {
            return split[0];
        }
        throw new FileNotFoundException("Could not find identity in document id");
    }

    public String getBucket(String documentId) throws FileNotFoundException {
        String[] split = documentId.split(DOCID_SEPERATOR, 4);
        if (split.length > 2) {
            return split[1];
        }
        throw new FileNotFoundException("Could not find bucket in document id");
    }

    public String getPrefix(String documentId) throws FileNotFoundException {
        String[] split = documentId.split(DOCID_SEPERATOR, 4);
        if (split.length > 2) {
            return split[2];
        }
        throw new FileNotFoundException("Could not find volume prefix in document id");
    }


    public String getFilePath(String documentId) throws FileNotFoundException {
        String[] split = documentId.split(DOCID_SEPERATOR, 4);
        if (split.length > 3 && split[3] != "") {
            return split[3];
        }
        throw new FileNotFoundException("Could not find file path in document id");
    }

    public List<String> splitPath(String filePath) {
        ArrayList<String> list = new ArrayList<>(Arrays.asList(filePath.split("/")));
        return list;
    }


    public String buildId(String identity, String bucket, String prefix, String filePath) {
        if (bucket != null && prefix != null && filePath != null) {
            return identity + DOCID_SEPERATOR + bucket
                    + DOCID_SEPERATOR + prefix + DOCID_SEPERATOR + filePath;
        } else if (bucket != null && prefix != null) {
            return identity + DOCID_SEPERATOR + bucket + DOCID_SEPERATOR + prefix;
        } else if (bucket != null) {
            return identity + DOCID_SEPERATOR + bucket;
        } else {
            return identity;
        }
    }
}
