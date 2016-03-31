package de.qabel.qabelbox.exceptions;

import de.qabel.core.exceptions.QblException;

public class QblStorageNameConflict extends QblException {
    public QblStorageNameConflict(Throwable e) {
        super(e);
    }

    public QblStorageNameConflict(String s) {
        super(s);
    }
}
