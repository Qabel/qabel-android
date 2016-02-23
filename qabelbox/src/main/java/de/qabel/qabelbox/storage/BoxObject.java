package de.qabel.qabelbox.storage;

import android.support.annotation.NonNull;

public abstract class BoxObject implements Comparable<BoxObject>, BottomSheetMenu {
	public String name;

	public BoxObject(String name) {this.name = name;}

	@Override
	public int compareTo(@NonNull BoxObject another) {
		return this.name.compareTo(another.name);
	}
}
