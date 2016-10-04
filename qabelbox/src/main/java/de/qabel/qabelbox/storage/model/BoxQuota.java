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

    @Override
    public boolean equals(Object o) {
        if (o instanceof BoxQuota) {
            BoxQuota other = (BoxQuota) o;
            if (other.quota == this.quota && other.size == this.size) {
                return true;
            }
        }
        return false;
    }
}
