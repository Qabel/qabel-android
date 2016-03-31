package de.qabel.qabelbox.exceptions;

/**
 * Wraps SqlExceptions for adaptable Databases
 */
public class QblPersistenceException extends RuntimeException {

    public Exception e;

    public QblPersistenceException(Exception e) {
        this.e = e;
    }

    public Exception getException() {
        return e;
    }

}
