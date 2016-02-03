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

    public String getIdentity(String documentId) throws FileNotFoundException {
        String[] split = documentId.split(BoxProvider.DOCID_SEPARATOR, 2);
        if (split.length > 1) {
            return split[0];
        }
        throw new FileNotFoundException("Could not find identity in document id");
    }

    public String getBucket(String documentId) throws FileNotFoundException {
        String[] split = documentId.split(BoxProvider.DOCID_SEPARATOR, 4);
        if (split.length > 2) {
            return split[1];
        }
        throw new FileNotFoundException("Could not find bucket in document id");
    }

    public String getPrefix(String documentId) throws FileNotFoundException {
        String[] split = documentId.split(BoxProvider.DOCID_SEPARATOR, 4);
        if (split.length > 2) {
            return split[2];
        }
        throw new FileNotFoundException("Could not find volume prefix in document id");
    }


    public String getFilePath(String documentId) throws FileNotFoundException {
        String[] split = documentId.split(BoxProvider.DOCID_SEPARATOR, 4);
        if (split.length > 3 && split[3] != "") {
            return split[3];
        }
        throw new FileNotFoundException("Could not find file path in document id");
    }

	public String getPath(String documentId) throws FileNotFoundException {
		String filepath = getFilePath(documentId);
		filepath = filepath.substring(0 , filepath.lastIndexOf('/') + 1);
		// TODO: Workaround for wrong formatted document IDs
		if (filepath.startsWith("//")) {
			return filepath.substring(1, filepath.length());
		}
		return filepath;
	}

    public List<String> splitPath(String filePath) {
        ArrayList<String> list = new ArrayList<>(Arrays.asList(filePath.split("/")));
        return list;
    }

    public String getBaseName(String documentID) throws FileNotFoundException {
        String filepath = getFilePath(documentID);
        return filepath.substring(filepath.lastIndexOf('/') + 1, filepath.length());
    }


    public String buildId(String identity, String bucket, String prefix, String filePath) {
        if (bucket != null && prefix != null && filePath != null) {
            return identity + BoxProvider.DOCID_SEPARATOR + bucket
                    + BoxProvider.DOCID_SEPARATOR + prefix + BoxProvider.DOCID_SEPARATOR + filePath;
        } else if (bucket != null && prefix != null) {
            return identity + BoxProvider.DOCID_SEPARATOR + bucket + BoxProvider.DOCID_SEPARATOR + prefix;
        } else if (bucket != null) {
            return identity + BoxProvider.DOCID_SEPARATOR + bucket;
        } else {
            return identity;
        }
    }
}
