package de.qabel.android.exceptions;

/**
 * Wraps SqlExceptions for adaptable Databases
 */
public class QblPersistenceException extends RuntimeException {

	public Exception causeException;

	public QblPersistenceException() {

	}

	public QblPersistenceException(Exception causeException) {
		this.causeException = causeException;
	}

	public Exception getException() {
		return causeException;
	}

}
