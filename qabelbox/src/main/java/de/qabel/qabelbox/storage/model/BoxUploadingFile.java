package de.qabel.qabelbox.storage.model;

import de.qabel.qabelbox.storage.model.BoxObject;

public class BoxUploadingFile extends BoxObject {

    private String path;
    private String ownerIdentifier;
    public long totalSize;
    public long uploadedSize;

    public BoxUploadingFile(String name, String path, String ownerIdentifier) {
        super(name);
        this.path = path;
        this.ownerIdentifier = ownerIdentifier;
    }

    public String getPath() {
        return path;
    }

    public String getOwnerIdentifier() {
        return ownerIdentifier;
    }

    public int getUploadStatusPercent() {
        if (totalSize == 0 || uploadedSize == 0) {
            return 0;
        }
        return (int) (100 * uploadedSize / totalSize);
    }
}
