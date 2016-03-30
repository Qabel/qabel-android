package de.qabel.qabelbox.exceptions;

public class QblStorageEntityExistsException extends QblStorageException {
    public QblStorageEntityExistsException(Throwable e) {
        super(e);
    }

    public QblStorageEntityExistsException(String s) {
        super(s);
    }

    public QblStorageEntityExistsException(String s, Throwable e) {
        super(s, e);
    }
}
