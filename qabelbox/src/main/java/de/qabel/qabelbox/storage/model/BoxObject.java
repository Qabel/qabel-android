package de.qabel.qabelbox.storage.model;

import android.support.annotation.NonNull;

public class BoxObject implements Comparable<BoxObject> {
    public String name;

    public BoxObject(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(@NonNull BoxObject another) {
        return this.name.compareTo(another.name);
    }
}
