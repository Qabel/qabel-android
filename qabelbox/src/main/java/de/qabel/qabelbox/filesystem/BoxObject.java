package de.qabel.qabelbox.filesystem;

import android.support.annotation.NonNull;

public class BoxObject implements Comparable<BoxObject> {
    private String name;
    private int shareCount;
    private int id;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getShareCount() {
        return shareCount;
    }

    public void setShareCount(int shareCount) {
        this.shareCount = shareCount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int compareTo(@NonNull BoxObject another) {
        if (this instanceof BoxFile && another instanceof BoxFile) {
            return this.getName().compareTo(another.getName());
        }
        if (this instanceof BoxFolder && another instanceof BoxFolder) {
            return this.getName().compareTo(another.getName());
        }
        if (this instanceof BoxFile) {
            return -1;
        }
        return 1;
    }
}
