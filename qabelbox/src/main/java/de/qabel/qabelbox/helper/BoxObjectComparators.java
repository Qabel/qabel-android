package de.qabel.qabelbox.helper;

import de.qabel.qabelbox.storage.BoxFile;
import de.qabel.qabelbox.storage.BoxFolder;
import de.qabel.qabelbox.storage.BoxObject;
import de.qabel.qabelbox.storage.BoxUploadingFile;

import java.util.Comparator;

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
