package de.qabel.android.persistence;

/**
 * Wraps the Database and allows to test with other databases.
 *
 * @param <T>
 */
public abstract class DatabaseWrapperImpl<T> implements DatabaseWrapper {

	protected static final String BLOB = "BLOB";
	protected static final String ID = "ID";
	protected static final String ID_QUERY = "ID = ?";

}
