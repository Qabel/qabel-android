package de.qabel.qabelbox.exceptions;

/**
 * Created by JoGir on 04.03.16.
 */
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
