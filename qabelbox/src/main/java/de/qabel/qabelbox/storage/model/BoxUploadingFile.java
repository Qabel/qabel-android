package de.qabel.qabelbox.storage.model;

public class BoxUploadingFile extends BoxObject {

    private String path;
    private String ownerIdentifier;
    public long totalSize = 1;
    public long uploadedSize = 0;

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

}
