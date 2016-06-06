package de.qabel.qabelbox.storage.model;

public class BoxQuota {

    private long size = 0;
    private long quota = -1;

    public long getSize() {
        return size;
    }

    public long getQuota() {
        return quota;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setQuota(long quota) {
        this.quota = quota;
    }

}
