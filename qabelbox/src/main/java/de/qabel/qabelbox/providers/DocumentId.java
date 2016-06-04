package de.qabel.qabelbox.providers;

import java.util.Arrays;

import de.qabel.desktop.StringUtils;

public class DocumentId {

    private static final String PATH_SEPARATOR = "/";

    private String identityKey;
    private String prefix;
    private String[] path;
    private String fileName;

    public DocumentId(String identityKey, String prefix, String[] path, String filename) {
        this.identityKey = identityKey;
        this.prefix = prefix;
        this.path = path;
        this.fileName = filename;
    }

    public String getIdentityKey() {
        return identityKey;
    }

    public String getPrefix() {
        return prefix;
    }

    public String[] getPath() {
        return path;
    }

    public String getPathString() {
        if(path.length == 0){
            return PATH_SEPARATOR;
        }
        return StringUtils.join(PATH_SEPARATOR, path);
    }

    public String getFilePath() {
        String[] parts = Arrays.copyOf(path, path.length + 1);
        parts[parts.length-1] = fileName;
        return StringUtils.join(PATH_SEPARATOR, parts);
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String toString() {
        return identityKey + BoxProvider.DOCID_SEPARATOR +
                prefix + BoxProvider.DOCID_SEPARATOR +
                getPathString() + PATH_SEPARATOR + fileName;
    }

}
