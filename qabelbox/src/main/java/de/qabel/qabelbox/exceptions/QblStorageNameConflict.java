package de.qabel.qabelbox.exceptions;

public class QblStorageNameConflict extends QblStorageException {
	public QblStorageNameConflict(Throwable e) {
		super(e);
	}

	public QblStorageNameConflict(String s) {
		super(s);
	}
}
