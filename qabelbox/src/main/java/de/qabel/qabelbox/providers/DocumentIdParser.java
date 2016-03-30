package de.qabel.qabelbox.providers;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Document IDs are built like this:
 * public-key::::prefix::::/filepath
 */
public class DocumentIdParser {
    public String getIdentity(String documentId) throws FileNotFoundException {
        String[] split = documentId.split(BoxProvider.DOCID_SEPARATOR, 2);
        if (split.length > 0 && split[0].length() > 0) {
            return split[0];
        }
        throw new FileNotFoundException("Could not find identity in document id");
    }


    public String getPrefix(String documentId) throws FileNotFoundException {
        String[] split = documentId.split(BoxProvider.DOCID_SEPARATOR, 4);
        if (split.length > 1 && split[1].length() > 0) {
            return split[1];
        }
        throw new FileNotFoundException("Could not find volume prefix in document id");
    }


    public String getFilePath(String documentId) throws FileNotFoundException {
        String[] split = documentId.split(BoxProvider.DOCID_SEPARATOR, 4);
        if (split.length > 2 && split[2].length() > 0) {
            return split[2];
        }
        throw new FileNotFoundException("Could not find file path in document id");
    }

    public String getPath(String documentId) throws FileNotFoundException {
        String filepath = getFilePath(documentId);
        filepath = filepath.substring(0, filepath.lastIndexOf('/') + 1);
        // TODO: Workaround for wrong formatted document IDs
        if (filepath.startsWith("//")) {
            return filepath.substring(1, filepath.length());
        }
        return filepath;
    }

    public List<String> splitPath(String filePath) {
        return new ArrayList<>(Arrays.asList(filePath.split("/")));
    }

    public String getBaseName(String documentID) throws FileNotFoundException {
        String filepath = getFilePath(documentID);
        return filepath.substring(filepath.lastIndexOf('/') + 1, filepath.length());
    }


    public String buildId(String identity, String prefix, String filePath) {
        if (prefix != null && filePath != null) {
            return identity + BoxProvider.DOCID_SEPARATOR + prefix + BoxProvider.DOCID_SEPARATOR + filePath;
        } else if (prefix != null) {
            return identity + BoxProvider.DOCID_SEPARATOR + prefix;
        } else {
            return identity;
        }
    }
}
