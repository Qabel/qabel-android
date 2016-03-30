package de.qabel.qabelbox.storage;

public class BoxUploadingFile extends BoxObject {
    public long totalSize;
    public long uploadedSize;

    public BoxUploadingFile(String name) {
        super(name);
    }

    public int getUploadStatusPercent() {
        if (totalSize == 0 || uploadedSize == 0) {
            return 0;
        }
        return (int) (100 * uploadedSize / totalSize);
    }
}
