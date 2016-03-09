package de.qabel.android.persistence;

/**
 * Wraps the Database and allows to test with other databases.
 *
 * @param <T>
 */
public abstract class DatabaseWrapperImpl<T> implements DatabaseWrapper {

	protected static final String STR_BLOB = "BLOB";
	protected static final String STR_ID = "ID";
	protected static final String STR_ID_QUERY = "ID = ?";

}
