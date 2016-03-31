package de.qabel.qabelbox.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

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
