package de.qabel.android.helper;

import java.util.Comparator;

import de.qabel.android.storage.BoxFile;
import de.qabel.android.storage.BoxFolder;
import de.qabel.android.storage.BoxObject;
import de.qabel.android.storage.BoxUploadingFile;

public class BoxObjectComparators {

	public static Comparator<BoxObject> alphabeticOrderDirectoriesFirstIgnoreCase() {
		return new Comparator<BoxObject>() {
			@Override
			public int compare(BoxObject lhs, BoxObject rhs) {
				if (lhs instanceof BoxFile && (rhs instanceof BoxFile || rhs instanceof BoxUploadingFile)) {
					return lhs.name.compareToIgnoreCase(rhs.name);
				}
				if (lhs instanceof BoxUploadingFile && (rhs instanceof BoxFile || rhs instanceof BoxUploadingFile)) {
					return lhs.name.compareToIgnoreCase(rhs.name);
				}
				if (lhs instanceof BoxFolder && rhs instanceof BoxFolder) {
					return lhs.name.compareToIgnoreCase(rhs.name);
				}
				if (lhs instanceof BoxFolder) {
					return -1;
				}
				return 1;
			}
		};
	}
}
