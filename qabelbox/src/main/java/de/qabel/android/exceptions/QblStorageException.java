package de.qabel.android.exceptions;

import de.qabel.core.exceptions.QblException;

public class QblStorageException extends QblException {
	public QblStorageException(Throwable e) {
		super(e.getMessage());
	}

	public QblStorageException(String s) {
		super(s);
	}

	public QblStorageException(String s, Throwable e) {
		super(s, e);
	}
}
